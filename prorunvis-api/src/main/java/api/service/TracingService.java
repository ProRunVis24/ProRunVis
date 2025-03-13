package api.service;

import com.github.javaparser.ast.CompilationUnit;
import org.springframework.stereotype.Service;
import prorunvis.CompileAndRun;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * This service:
 *  1) Reads base64-encoded instrumented code from
 *     resources/local_storage/session-<sessionId>/<instrumentId>/instrumented_base64.txt
 *  2) Decodes/unzips into resources/out/session-<sessionId>/downloaded_instrumented
 *  3) Compiles and runs the code => Trace.tr
 *  4) Copies Trace.tr into local_storage/session-<sessionId>/<instrumentId>/Trace.tr
 */
@Service
public class TracingService {

    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";

    public TracingService() {
        // no DB repos needed
    }

    /**
     * Clears out resources/out/session-<sessionId> to ensure a fresh decode/unzip + compile/run.
     */
    private void cleanOutputDirectories(String sessionId) {
        File outDir = new File("resources/out/session-" + sessionId);
        if (outDir.exists()) {
            try {
                org.springframework.util.FileSystemUtils.deleteRecursively(outDir.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean output directories for session: " + sessionId, e);
            }
        }
        if (!outDir.mkdirs()) {
            throw new RuntimeException("Failed to create output directory after cleaning for session: " + sessionId);
        }
    }

    /**
     * Decode, compile, run, produce Trace.tr inside local_storage/session-<sessionId>/<instrumentId>.
     */
    public void runTrace(String instrumentId, String sessionId) {
        // local_storage/session-<sessionId>/<instrumentId>
        File localIdFolder = new File(LOCAL_STORAGE_DIR + "/session-" + sessionId, instrumentId);
        if (!localIdFolder.exists() || !localIdFolder.isDirectory()) {
            throw new RuntimeException("Local ID folder does not exist: " + localIdFolder.getAbsolutePath());
        }

        // local_storage/session-<sessionId>/<instrumentId>/instrumented_base64.txt
        File base64File = new File(localIdFolder, "instrumented_base64.txt");
        if (!base64File.exists()) {
            throw new RuntimeException("No instrumented_base64.txt found for ID: " + instrumentId + " in session: " + sessionId);
        }

        // 1) Read the base64 content
        String base64 = Util.readFileAsString(base64File);

        // 2) decode/unzip into resources/out/session-<sessionId>/downloaded_instrumented
        cleanOutputDirectories(sessionId);
        String sessionOutDir = "resources/out/session-" + sessionId;
        File instrumentedDir = Util.unzipAndDecode(base64, sessionOutDir);

        // 3) compile + run
        List<CompilationUnit> cus = Util.loadCUs(instrumentedDir);
        try {
            CompileAndRun.run(
                    cus,
                    instrumentedDir.getAbsolutePath(),
                    instrumentedDir.getAbsolutePath() + "/compiled"
            );
        } catch (Exception e) {
            throw new RuntimeException("Trace run failed for session: " + sessionId, e);
        }

        // 4) check for Trace.tr INSIDE THE "compiled" SUBFOLDER
        File compiledFolder = new File(instrumentedDir, "compiled");
        File traceFile = new File(compiledFolder, "Trace.tr");

        if (!traceFile.exists()) {
            throw new RuntimeException(
                    "No Trace.tr found in: " + compiledFolder.getAbsolutePath());
        }
        System.out.println("Trace file generated at: " + traceFile.getAbsolutePath() + " for session: " + sessionId);

        // 5) copy Trace.tr → local_storage/session-<sessionId>/<instrumentId>/Trace.tr
        File localTrace = new File(localIdFolder, "Trace.tr");
        try {
            Files.copy(
                    traceFile.toPath(),
                    localTrace.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed copying Trace.tr to: " + localTrace.getAbsolutePath() + " for session: " + sessionId,
                    e
            );
        }
    }

    // For backward compatibility - delegates to the session-aware version
    public void runTrace(String instrumentId) {
        // Default session ID for backward compatibility
        runTrace(instrumentId, "default");
    }
}