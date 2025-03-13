package api.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Reworked VisualizationService to read from project-specific locations
 */
@Service
public class VisualizationService {

    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";

    public VisualizationService() {
        // no repos needed
    }

    public String getTraceJson(String localId, String projectId) {
        // find resources/local_storage/project-<projectId>/<localId>/processedTrace.json
        File localFolder = new File(LOCAL_STORAGE_DIR + "/project-" + projectId, localId);
        if (!localFolder.exists() || !localFolder.isDirectory()) {
            throw new RuntimeException("Local ID folder not found for project: " + projectId +
                    " at: " + localFolder.getAbsolutePath());
        }
        File processedFile = new File(localFolder, "processedTrace.json");
        if (!processedFile.exists()) {
            throw new RuntimeException("No processedTrace.json found for ID: " + localId +
                    " in project: " + projectId);
        }

        try {
            byte[] content = Files.readAllBytes(processedFile.toPath());
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read processedTrace.json for project: " + projectId, e);
        }
    }
}