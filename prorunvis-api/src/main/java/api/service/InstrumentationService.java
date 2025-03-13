package api.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.utils.ProjectRoot;
import org.springframework.stereotype.Service;
import prorunvis.instrument.Instrumenter;
import prorunvis.preprocess.Preprocessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reworked InstrumentationService that stores instrumented code in session-specific locations
 */
@Service
public class InstrumentationService {

    /**
     * The base folder where we store each run's local data,
     * keyed by a session ID and random instrument ID.
     */
    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";

    public InstrumentationService() {
        // No repository injection needed
    }

    /**
     * Creates/cleans session-specific output directories
     */
    private void ensureCleanOutputDirectories(String sessionId) {
        File outDir = new File("resources/out/session-" + sessionId);
        if (outDir.exists()) {
            System.out.println("Cleaning existing resources/out/session-" + sessionId + " directory...");
            try {
                Files.walk(outDir.toPath())
                        .map(java.nio.file.Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2)) // delete children first
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean output directories for session: " + sessionId, e);
            }
        }
        if (!outDir.mkdirs()) {
            throw new RuntimeException("Failed to create output directory for session: " + sessionId);
        }

        File instrDir = new File(outDir, "instrumented");
        if (!instrDir.exists() && !instrDir.mkdirs()) {
            throw new RuntimeException("Failed to create instrumented directory for session: " + sessionId);
        }
    }

    /**
     * Instruments the code and stores results in session-specific locations
     *
     * @param projectName   the name of the user's project
     * @param inputDirPath  the folder containing the source code to be instrumented
     * @param randomId      a unique ID for this instrumentation job
     * @param sessionId     the session identifier
     * @return Some success message (or path)
     */
    public String instrumentProject(String projectName,
                                    String inputDirPath,
                                    String randomId,
                                    String sessionId) {

        // 1) Verify input directory is valid
        File inputDir = new File(inputDirPath);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new RuntimeException("Input directory does not exist or is not a directory: " + inputDirPath);
        }

        // 2) Clean & set up session-specific output directory
        ensureCleanOutputDirectories(sessionId);

        // 3) Parse & instrument code
        System.out.println("Parsing project at: " + inputDirPath + " for session: " + sessionId);
        ProjectRoot projectRoot = Util.parseProject(inputDir);
        List<CompilationUnit> cus = Util.getCUs(projectRoot);
        if (cus.isEmpty()) {
            throw new RuntimeException("No Java files found in: " + inputDirPath);
        }
        System.out.println("Found " + cus.size() + " compilation units for session: " + sessionId);

        Map<Integer, Node> map = new HashMap<>();
        for (CompilationUnit cu : cus) {
            Preprocessor.run(cu);
            Instrumenter.run(cu, map);
        }

        // 4) Save instrumented code to the session directory
        String sessionInstrDir = "resources/out/session-" + sessionId + "/instrumented";
        Instrumenter.saveInstrumented(projectRoot, sessionInstrDir);

        // 5) Check that something was indeed saved
        File instrDir = new File(sessionInstrDir);
        String[] instrumentedFiles = instrDir.list();
        if (instrumentedFiles == null || instrumentedFiles.length == 0) {
            throw new RuntimeException("No files found in instrumented directory after saving instrumented code for session: " + sessionId);
        }
        System.out.println("Files in instrumented directory for session: " + sessionId);
        for (String f : instrumentedFiles) {
            System.out.println(" - " + f);
        }

        // 6) Zip & encode
        System.out.println("Zipping instrumented code for session: " + sessionId);
        String zipBase64 = Util.zipAndEncode(projectRoot, sessionId);
        if (zipBase64 == null || zipBase64.isEmpty()) {
            throw new RuntimeException("Failed to zip instrumented code for session: " + sessionId);
        }

        // 7) Create session-specific folder structure
        String sessionBaseDir = LOCAL_STORAGE_DIR + "/session-" + sessionId;
        File sessionBaseDirFile = new File(sessionBaseDir);
        if (!sessionBaseDirFile.exists()) {
            sessionBaseDirFile.mkdirs();
        }

        // 8) Store in session-specific location
        File randomIdFolder = new File(sessionBaseDir, randomId);
        if (!randomIdFolder.exists()) {
            randomIdFolder.mkdirs();
        }
        File outputFile = new File(randomIdFolder, "instrumented_base64.txt");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(zipBase64.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error writing instrumented file for session: " + sessionId, e);
        }

        System.out.println("Instrumented code stored locally in: " + outputFile.getAbsolutePath() + " for session: " + sessionId);

        // Return a success message
        return "Instrumented code saved under ID=" + randomId + " for session=" + sessionId;
    }

    // For backward compatibility
    public String instrumentProject(String projectName, String inputDirPath, String randomId) {
        return instrumentProject(projectName, inputDirPath, randomId, "default");
    }
}