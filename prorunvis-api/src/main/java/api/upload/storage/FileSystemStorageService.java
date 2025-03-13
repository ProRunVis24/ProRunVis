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

/**
 * A Storage service used to store given data in a directory specified
 * through a {@link StorageProperties} element.
 */
@Service
public class FileSystemStorageService implements StorageService {

    /**
     * Base path to rootLocation.
     */
    private final Path rootLocation;

    /**
     * Base path to out location.
     */
    private final Path outLocation;

    /**
     * Base path to local storage location.
     */
    private final Path localStorageLocation;

    /**
     * @param properties The storage properties for the storage service
     */
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

    /**
     * Initializes the storage service by creating the base folders.
     */
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

    /**
     * Initializes storage for a specific session
     *
     * @param sessionId The unique session identifier
     */

    public void initSession(String sessionId) {
        try {
            Path sessionInPath = getSessionInPath(sessionId);
            Path sessionOutPath = getSessionOutPath(sessionId);
            Path sessionLocalStoragePath = getSessionLocalStoragePath(sessionId);

            Files.createDirectories(sessionInPath);
            Files.createDirectories(sessionOutPath);
            Files.createDirectories(sessionLocalStoragePath);

            System.out.println("Initialized directories for session: " + sessionId);
            System.out.println("  - Input: " + sessionInPath);
            System.out.println("  - Output: " + sessionOutPath);
            System.out.println("  - Local Storage: " + sessionLocalStoragePath);

        } catch (IOException e) {
            throw new StorageException("Could not create session directories for session: " + sessionId, e);
        }
    }

    /**
     * Get the path to the session's input directory
     */
    private Path getSessionInPath(String sessionId) {
        return rootLocation.resolve("session-" + sessionId);
    }

    /**
     * Get the path to the session's output directory
     */
    private Path getSessionOutPath(String sessionId) {
        return outLocation.resolve("session-" + sessionId);
    }

    /**
     * Get the path to the session's local storage directory
     */
    private Path getSessionLocalStoragePath(String sessionId) {
        return localStorageLocation.resolve("session-" + sessionId);
    }

    /**
     * Returns the input location path for a session
     */

    public String getSessionInLocation(String sessionId) {
        return getSessionInPath(sessionId).toString();
    }

    /**
     * Returns the output location path for a session
     */

    public String getSessionOutLocation(String sessionId) {
        return getSessionOutPath(sessionId).toString();
    }

    /**
     * Returns the local storage path for a session
     */

    public String getSessionLocalStorageLocation(String sessionId) {
        return getSessionLocalStoragePath(sessionId).toString();
    }

    /**
     * Stores the contents of a file to a new {@link java.io.File}
     * in the session-specific directory.
     * @param part a part of a http-request representing a file to be stored
     * @param sessionId the session identifier
     */

    public void store(final Part part, final String sessionId) {
        try {
            String fileName = FilenameUtils.separatorsToSystem(part.getSubmittedFileName());
            Path sessionPath = getSessionInPath(sessionId);
            Path file = sessionPath.resolve(fileName);

            if (Files.notExists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }

            try (InputStream inputStream = part.getInputStream()) {
                Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Stored file " + fileName + " for session: " + sessionId + " at: " + file);

        } catch (IOException e) {
            throw new StorageException("Could not store file for session: " + sessionId, e);
        }
    }

    /**
     * The original store method without session ID - kept for backward compatibility
     * Will be removed once all code is updated to use the session-aware version
     */

    public void store(final Part part) {
        throw new StorageException("Session ID is required for file storage. Use store(part, sessionId) instead.");
    }

    /**
     * Recursively deletes all files in the directories
     */
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

    /**
     * Deletes all data for a specific session
     *
     * @param sessionId The session identifier whose data should be deleted
     */
    @Override
    public void deleteAllForSession(String sessionId) {
        Path sessionInPath = getSessionInPath(sessionId);
        Path sessionOutPath = getSessionOutPath(sessionId);
        Path sessionLocalStoragePath = getSessionLocalStoragePath(sessionId);

        System.out.println("Deleting files for session: " + sessionId);
        System.out.println("  - From: " + sessionInPath);
        System.out.println("  - From: " + sessionOutPath);
        System.out.println("  - From: " + sessionLocalStoragePath);

        FileSystemUtils.deleteRecursively(sessionInPath.toFile());
        FileSystemUtils.deleteRecursively(sessionOutPath.toFile());
        // We don't delete the session local storage as it may contain results
        // from previous runs that the user still wants to view

        try {
            Files.createDirectories(sessionInPath);
            Files.createDirectories(sessionOutPath);
            Files.createDirectories(sessionLocalStoragePath);
        } catch (IOException e) {
            throw new StorageException("Could not recreate session directories after deletion for session: " + sessionId, e);
        }
    }
}