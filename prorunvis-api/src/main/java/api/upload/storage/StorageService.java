package api.upload.storage;

import jakarta.servlet.http.Part;

/**
 * A Service for storing data provided from an HTTP request.
 */
public interface StorageService {

    void init();
    void initSession(String sessionId);

    /**
<<<<<<< Updated upstream
     * For backward compatibility.  (We no longer call this directly.)
=======
     * Initializes storage for a specific session
     *
     * @param sessionId The unique session identifier
     */
    void initSession(String sessionId);

    /**
     * Stores data to the given storage location. This method should not be
     * called without {@link #init()} having been called before.
     * @param part The data to store. The data is provided in form of a
     *             {@link Part} from a http request.
>>>>>>> Stashed changes
     */
    void store(Part part, String sessionId);

    /**
<<<<<<< Updated upstream
     * **NEW** Overload that stores the file in sessionId/localId subfolder.
=======
     * Stores data to the session-specific storage location.
     * @param part The data to store. The data is provided in form of a
     *             {@link Part} from a http request.
     * @param sessionId The session identifier to associate the stored data with
     */
    void store(Part part, String sessionId);

    /**
     * Deletes all data stored by this storage service or storage services
     * which are providing the same storage location.
>>>>>>> Stashed changes
     */
    void store(Part part, String sessionId, String localId);

    void deleteAll();
<<<<<<< Updated upstream
    void deleteAllForSession(String sessionId);

    String getSessionInLocation(String sessionId);
    String getSessionOutLocation(String sessionId);
=======

    /**
     * Deletes all data for a specific session
     *
     * @param sessionId The session identifier whose data should be deleted
     */
    void deleteAllForSession(String sessionId);

    /**
     * Gets the input location path for a specific session
     *
     * @param sessionId The session identifier
     * @return The path to the session's input location
     */
    String getSessionInLocation(String sessionId);

    /**
     * Gets the output location path for a specific session
     *
     * @param sessionId The session identifier
     * @return The path to the session's output location
     */
    String getSessionOutLocation(String sessionId);

    /**
     * Gets the local storage path for a specific session
     *
     * @param sessionId The session identifier
     * @return The path to the session's local storage location
     */
    String getSessionLocalStorageLocation(String sessionId);
>>>>>>> Stashed changes
}