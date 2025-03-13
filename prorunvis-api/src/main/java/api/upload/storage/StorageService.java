package api.upload.storage;

import jakarta.servlet.http.Part;

/**
 * A Service for storing data provided from a http request.
 * The user of this interface can create and delete all
 * necessary directories and has control of the way each {@link Part}
 * of the request is stored.
 */
public interface StorageService {

    /**
     * Initializes the storage location and structure used by this storage
     * service element.
     */
    void init();

    /**
     * Initializes storage for a specific project
     *
     * @param projectId The unique project identifier
     */
    void initProject(String projectId);

    /**
     * Stores data to the project-specific storage location.
     * @param part The data to store. The data is provided in form of a
     *             {@link Part} from a http request.
     * @param projectId The project identifier to associate the stored data with
     */
    void store(Part part, String projectId);

    /**
     * Deletes all data stored by this storage service or storage services
     * which are providing the same storage location.
     */
    void deleteAll();

    /**
     * Deletes all data for a specific project
     *
     * @param projectId The project identifier whose data should be deleted
     */
    void deleteAllForProject(String projectId);

    /**
     * Gets the input location path for a specific project
     *
     * @param projectId The project identifier
     * @return The path to the project's input location
     */
    String getProjectInLocation(String projectId);

    /**
     * Gets the output location path for a specific project
     *
     * @param projectId The project identifier
     * @return The path to the project's output location
     */
    String getProjectOutLocation(String projectId);

    /**
     * Gets the local storage path for a specific project
     *
     * @param projectId The project identifier
     * @return The path to the project's local storage location
     */
    String getProjectLocalStorageLocation(String projectId);
}