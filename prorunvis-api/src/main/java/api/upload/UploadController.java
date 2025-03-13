package api.upload;

import api.upload.storage.StorageException;
import api.upload.storage.StorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
<<<<<<< Updated upstream
=======
import jakarta.servlet.http.HttpServletResponse;
>>>>>>> Stashed changes
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Controller
public class UploadController {

    private final StorageService storageService;

    @Autowired
    public UploadController(final StorageService storageService) {
        this.storageService = storageService;
    }

<<<<<<< Updated upstream
    @GetMapping("/")
    public String getIndex(HttpServletRequest request) {
        // Create or retrieve session ID.
        HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        // Initialize session directories if new.
        storageService.initSession(sessionId);
=======
    /**
     * Handles hosting the default landing page. Always hosts
     * index.html on the default path "/".
     *
     * @return A String representing the file to be hosted, which can
     * be used by the thymeleaf plugin
     */
    @GetMapping("/")
    public String getIndex(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestParam(required = false) Boolean newSession) {

        HttpSession session;

        if (Boolean.TRUE.equals(newSession)) {
            // Force creation of a new session if explicitly requested
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.invalidate();
            }

            session = request.getSession(true);
            String projectId = UUID.randomUUID().toString().substring(0, 8);
            session.setAttribute("projectId", projectId);
            System.out.println("Created new session via newSession parameter: " + session.getId() + " with project ID: " + projectId);
        } else {
            // Get existing session or create a new one
            session = request.getSession(true);

            // Initialize project ID if not set
            if (session.getAttribute("projectId") == null) {
                String projectId = UUID.randomUUID().toString().substring(0, 8);
                session.setAttribute("projectId", projectId);
                System.out.println("Initialized project ID for existing session: " + session.getId() + " with project ID: " + projectId);
            }
        }

        // Initialize session storage
        storageService.initSession(session.getId());

        // Set cache control headers to prevent caching
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

>>>>>>> Stashed changes
        return "index.html";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public String handleUpload(final HttpServletRequest request) {
<<<<<<< Updated upstream
=======
        // Get the session ID
>>>>>>> Stashed changes
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new StorageException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();
<<<<<<< Updated upstream
        // Generate a new unique localId for this project.
        String localId = UUID.randomUUID().toString();

        try {
            for (Part part : request.getParts()) {
                // Store each file under resources/in/session-<sessionId>/<localId>/...
                storageService.store(part, sessionId, localId);
=======
        String projectId = (String) session.getAttribute("projectId");

        // Clear any existing files for this session only
        storageService.deleteAllForSession(sessionId);

        try {
            for (Part part : request.getParts()) {
                storageService.store(part, sessionId);
>>>>>>> Stashed changes
            }
        } catch (IOException | ServletException e) {
            throw new StorageException("Failed to upload files. " + e.getMessage());
        }
<<<<<<< Updated upstream
        // Return the generated localId so the client can use it in subsequent calls.
        return localId;
=======

        // Return the project ID so clients can see which project they're working with
        return "Upload successful for project: " + projectId;
>>>>>>> Stashed changes
    }

    @ExceptionHandler(StorageException.class)
    @ResponseBody
    public String handleException(final StorageException e) {
        String error = e.getMessage() + "\n";
        if (e.getCause() != null) {
            error += "\n" + e.getCause() + "\n";
        }
        return error;
    }
}