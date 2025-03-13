// FileSystemStorageService.java
package api.upload.storage;

import jakarta.servlet.http.Part;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileSystemStorageService implements StorageService {
    private final Path rootLocation;
    private final Path outLocation;
    private final Path localStorageLocation;

    public FileSystemStorageService(final StorageProperties properties) {
        if (properties.getLocation().trim().isEmpty()) {
            throw new StorageException("File storage directory cannot be empty.");
        }
        this.rootLocation = Paths.get(properties.getLocation());
        if (properties.getOutLocation().trim().isEmpty()) {
            this.outLocation = Paths.get("resources/out");
        } else {
            this.outLocation = Paths.get(properties.getOutLocation());
        }
        this.localStorageLocation = Paths.get("resources/local_storage");
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(outLocation);
            Files.createDirectories(localStorageLocation);
        } catch (IOException e) {
            throw new StorageException("Could not create directory.", e);
        }
    }

    @Override
    public void initProject(String projectId) {
        try {
            Path projectInPath = getProjectInPath(projectId);
            Path projectOutPath = getProjectOutPath(projectId);
            Path projectLocalStoragePath = getProjectLocalStoragePath(projectId);

            Files.createDirectories(projectInPath);
            Files.createDirectories(projectOutPath);
            Files.createDirectories(projectLocalStoragePath);

            System.out.println("Initialized directories for project: " + projectId);
            System.out.println("  - Input: " + projectInPath);
            System.out.println("  - Output: " + projectOutPath);
            System.out.println("  - Local Storage: " + projectLocalStoragePath);
        } catch (IOException e) {
            throw new StorageException("Could not create project directories for project: " + projectId, e);
        }
    }

    private Path getProjectInPath(String projectId) {
        return rootLocation.resolve("project-" + projectId);
    }

    private Path getProjectOutPath(String projectId) {
        return outLocation.resolve("project-" + projectId);
    }

    private Path getProjectLocalStoragePath(String projectId) {
        return localStorageLocation.resolve("project-" + projectId);
    }

    @Override
    public String getProjectInLocation(String projectId) {
        return getProjectInPath(projectId).toString();
    }

    @Override
    public String getProjectOutLocation(String projectId) {
        return getProjectOutPath(projectId).toString();
    }

    @Override
    public String getProjectLocalStorageLocation(String projectId) {
        return getProjectLocalStoragePath(projectId).toString();
    }

    @Override
    public void store(final Part part, final String projectId) {
        try {
            String fileName = FilenameUtils.separatorsToSystem(part.getSubmittedFileName());
            Path projectPath = getProjectInPath(projectId);
            Path file = projectPath.resolve(fileName);

            if (Files.notExists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }
            try (InputStream inputStream = part.getInputStream()) {
                Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Stored file " + fileName + " for project: " + projectId + " at: " + file);
        } catch (IOException e) {
            throw new StorageException("Could not store file for project: " + projectId, e);
        }
    }


    public void store(final Part part) {
        throw new StorageException("Project ID is required for file storage. Use store(part, projectId) instead.");
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
        FileSystemUtils.deleteRecursively(outLocation.toFile());
        FileSystemUtils.deleteRecursively(localStorageLocation.toFile());
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(outLocation);
            Files.createDirectories(localStorageLocation);
        } catch (IOException e) {
            throw new StorageException("Could not recreate directories after deletion.", e);
        }
    }

    @Override
    public void deleteAllForProject(String projectId) {
        Path projectInPath = getProjectInPath(projectId);
        Path projectOutPath = getProjectOutPath(projectId);
        Path projectLocalStoragePath = getProjectLocalStoragePath(projectId);

        System.out.println("Deleting files for project: " + projectId);
        FileSystemUtils.deleteRecursively(projectInPath.toFile());
        FileSystemUtils.deleteRecursively(projectOutPath.toFile());

        try {
            Files.createDirectories(projectInPath);
            Files.createDirectories(projectOutPath);
            Files.createDirectories(projectLocalStoragePath);
        } catch (IOException e) {
            throw new StorageException("Could not recreate project directories after deletion for project: " + projectId, e);
        }
    }
}