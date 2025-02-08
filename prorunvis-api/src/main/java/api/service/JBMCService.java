package api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * JBMCService spawns JBMC processes, captures their JSON output,
 * and parses variable assignments.
 */
@Service
public class JBMCService {

    // Directory where JBMC JSON output is stored (keyed by localId)
    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";
    // Fixed directory where the compiled .class files (or jar) are stored.
    private static final String COMPILED_OUTPUT_DIR = "resources/out/downloaded_instrumented/compiled";

    /**
     * Run JBMC on a specific method signature (e.g. "SnowWhite.indexMax:([I)I"),
     * using the compiled .class files at COMPILED_OUTPUT_DIR.
     *
     * @param localId         A unique identifier to store JBMC output.
     * @param methodSignature The fully qualified method signature for JBMC.
     * @param maxUnwind       The maximum unwind value.
     * @param maxArrayLength  The maximum nondeterministic array length.
     */
    public void runJBMC(String localId,
                        String methodSignature,
                        int maxUnwind,
                        int maxArrayLength) {

        System.out.println("DEBUG JBMCService.runJBMC: localId=" + localId
                + ", methodSignature=" + methodSignature
                + ", unwind=" + maxUnwind
                + ", maxArrayLength=" + maxArrayLength);

        // Prepare the local storage folder for this localId.
        File localFolder = new File(LOCAL_STORAGE_DIR, localId);
        if (!localFolder.exists() && !localFolder.mkdirs()) {
            throw new JBMCException("Failed to create local storage directory at: " + localFolder.getAbsolutePath());
        }

        // Use the fixed compiled directory.
        File compiledDir = new File(COMPILED_OUTPUT_DIR);
        System.out.println("DEBUG: compiledDir=" + compiledDir.getAbsolutePath()
                + ", exists? " + compiledDir.exists());
        if (!compiledDir.exists()) {
            throw new JBMCException("No compiled directory found at: " + compiledDir.getAbsolutePath());
        }

        // Build the JBMC command.
        String jbmcPath = "jbmc"; // or set an absolute path to the jbmc binary
        String[] cmd = new String[] {
                jbmcPath,
                methodSignature,
                "--classpath", compiledDir.getAbsolutePath(),
                "--unwind", String.valueOf(maxUnwind),
                "--unwinding-assertions",
                "--disable-uncaught-exception-check",
                "--throw-runtime-exceptions",
                "--max-nondet-array-length", String.valueOf(maxArrayLength),
                "--json-ui",

        };

        System.out.println("DEBUG: about to run JBMC with command: " + String.join(" ", cmd));

        // Spawn the JBMC process.
        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(compiledDir);  // Run from the compiled directory.
            process = pb.start();
            System.out.println("DEBUG: JBMC process started");
        } catch (IOException e) {
            System.out.println("DEBUG ERROR: Could not start JBMC process: " + e.getMessage());
            throw new JBMCException("Failed to start JBMC process: " + e.getMessage(), e);
        }

        // Capture the JBMC JSON output.
        StringBuilder jbmcOutput = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                jbmcOutput.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("DEBUG ERROR: Could not read JBMC stdout: " + e.getMessage());
            throw new JBMCException("Failed reading JBMC output: " + e.getMessage(), e);
        }

        // Wait for the JBMC process to finish.
        try {
            int exitCode = process.waitFor();
            System.out.println("DEBUG: JBMC exit code=" + exitCode);
            // Allow exit codes 0 (no counterexample) and 10 (counterexample found).
            if (exitCode != 0 && exitCode != 10) {
                // Read error stream.
                StringBuilder errStr = new StringBuilder();
                try (BufferedReader errBr = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errLine;
                    while ((errLine = errBr.readLine()) != null) {
                        errStr.append(errLine).append("\n");
                    }
                } catch (IOException ee) {
                    throw new JBMCException("Failed reading JBMC error output: " + ee.getMessage(), ee);
                }
                throw new JBMCException("JBMC process returned unexpected exit code " + exitCode
                        + ". Error: " + errStr);
            }
        } catch (InterruptedException e) {
            System.out.println("DEBUG ERROR: JBMC process interrupted: " + e.getMessage());
            throw new JBMCException("JBMC process was interrupted: " + e.getMessage(), e);
        }

        // Save the raw JBMC JSON to LOCAL_STORAGE_DIR/<localId>/jbmcOutput.json.
        File jbmcJsonFile = new File(localFolder, "jbmcOutput.json");
        System.out.println("DEBUG: writing JBMC result to " + jbmcJsonFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(jbmcJsonFile)) {
            fos.write(jbmcOutput.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("DEBUG ERROR: Could not write jbmcOutput.json: " + e.getMessage());
            throw new JBMCException("Failed to write JBMC JSON to file: " + e.getMessage(), e);
        }

        // Optionally parse the JSON (for example, to extract variable assignments).
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jbmcOutput.toString());
            System.out.println("DEBUG: JBMC JSON root parsed, top-level fields:");
            root.fieldNames().forEachRemaining(System.out::println);
        } catch (IOException e) {
            System.out.println("DEBUG ERROR: Could not parse JBMC JSON: " + e.getMessage());
            throw new JBMCException("Failed to parse JBMC JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Return the raw JBMC JSON from LOCAL_STORAGE_DIR/<localId>/jbmcOutput.json,
     * so the frontend can display or further process it.
     */
    public String getJBMCOutput(String localId) {
        File localFolder = new File(LOCAL_STORAGE_DIR, localId);
        File jbmcJsonFile = new File(localFolder, "jbmcOutput.json");
        System.out.println("DEBUG JBMCService.getJBMCOutput: localId=" + localId
                + ", file=" + jbmcJsonFile.getAbsolutePath()
                + ", exists? " + jbmcJsonFile.exists());

        if (!jbmcJsonFile.exists()) {
            throw new JBMCException("No jbmcOutput.json found for ID: " + localId);
        }

        try {
            byte[] data = java.nio.file.Files.readAllBytes(jbmcJsonFile.toPath());
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JBMCException("Failed to read jbmcOutput.json", e);
        }
    }
}