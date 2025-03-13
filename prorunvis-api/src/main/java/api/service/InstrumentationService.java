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
 * A reworked InstrumentationService that stores instrumented code in project-specific locations
 */
@Service
public class InstrumentationService {

    /**
     * The base folder where we store each run's local data,
     * keyed by a project ID and random instrument ID.
     */
    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";

    public InstrumentationService() {
        // No repository injection needed
    }

    /**
     * Creates/cleans project-specific output directories
     */
    private void ensureCleanOutputDirectories(String projectId) {
        File outDir = new File("resources/out/project-" + projectId);
        if (outDir.exists()) {
            System.out.println("Cleaning existing resources/out/project-" + projectId + " directory...");
            try {
                Files.walk(outDir.toPath())
                        .map(java.nio.file.Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2)) // delete children first
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean output directories for project: " + projectId, e);
            }
        }
        if (!outDir.mkdirs()) {
            throw new RuntimeException("Failed to create output directory for project: " + projectId);
        }

        File instrDir = new File(outDir, "instrumented");
        if (!instrDir.exists() && !instrDir.mkdirs()) {
            throw new RuntimeException("Failed to create instrumented directory for project: " + projectId);
        }
    }

    /**
     * Instruments the code and stores results in project-specific locations
     *
     * @param projectName   the name of the user's project
     * @param inputDirPath  the folder containing the source code to be instrumented
     * @param randomId      a unique ID for this instrumentation job
     * @param projectId     the project identifier
     * @return Some success message (or path)
     */
    public String instrumentProject(String projectName,
                                    String inputDirPath,
                                    String randomId,
                                    String projectId) {

        // 1) Verify input directory is valid
        File inputDir = new File(inputDirPath);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new RuntimeException("Input directory does not exist or is not a directory: " + inputDirPath);
        }

        // 2) Clean & set up project-specific output directory
        ensureCleanOutputDirectories(projectId);

        // 3) Parse & instrument code
        System.out.println("Parsing project at: " + inputDirPath + " for project: " + projectId);
        ProjectRoot projectRoot = Util.parseProject(inputDir);
        List<CompilationUnit> cus = Util.getCUs(projectRoot);
        if (cus.isEmpty()) {
            throw new RuntimeException("No Java files found in: " + inputDirPath);
        }
        System.out.println("Found " + cus.size() + " compilation units for project: " + projectId);

        Map<Integer, Node> map = new HashMap<>();
        for (CompilationUnit cu : cus) {
            Preprocessor.run(cu);
            Instrumenter.run(cu, map);
        }

        // 4) Save instrumented code to the project directory
        String projectInstrDir = "resources/out/project-" + projectId + "/instrumented";
        Instrumenter.saveInstrumented(projectRoot, projectInstrDir);

        // 5) Check that something was indeed saved
        File instrDir = new File(projectInstrDir);
        String[] instrumentedFiles = instrDir.list();
        if (instrumentedFiles == null || instrumentedFiles.length == 0) {
            throw new RuntimeException("No files found in instrumented directory after saving instrumented code for project: " + projectId);
        }
        System.out.println("Files in instrumented directory for project: " + projectId);
        for (String f : instrumentedFiles) {
            System.out.println(" - " + f);
        }

        // 6) Zip & encode
        System.out.println("Zipping instrumented code for project: " + projectId);
        String zipBase64 = Util.zipAndEncode(projectRoot, projectId);
        if (zipBase64 == null || zipBase64.isEmpty()) {
            throw new RuntimeException("Failed to zip instrumented code for project: " + projectId);
        }

        // 7) Create project-specific folder structure
        String projectBaseDir = LOCAL_STORAGE_DIR + "/project-" + projectId;
        File projectBaseDirFile = new File(projectBaseDir);
        if (!projectBaseDirFile.exists()) {
            projectBaseDirFile.mkdirs();
        }

        // 8) Store in project-specific location
        File randomIdFolder = new File(projectBaseDir, randomId);
        if (!randomIdFolder.exists()) {
            randomIdFolder.mkdirs();
        }
        File outputFile = new File(randomIdFolder, "instrumented_base64.txt");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(zipBase64.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error writing instrumented file for project: " + projectId, e);
        }

        System.out.println("Instrumented code stored locally in: " + outputFile.getAbsolutePath() + " for project: " + projectId);

        // Return a success message
        return "Instrumented code saved under ID=" + randomId + " for project=" + projectId;
    }
}