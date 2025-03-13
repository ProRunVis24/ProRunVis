package api.upload;

import api.upload.storage.StorageException;
import api.upload.storage.StorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

/**
 * A {@link Controller} used for handling files, that are uploaded
 * to the server.
 */
@Controller
public class UploadController {

    /**
     * The storage service used by this controller.
     */
    private final StorageService storageService;

    /**
     * @param storageService The storage service which this controller
     *                       will use for handling file storage for
     *                       uploaded files.
     */
    @Autowired
    public UploadController(final StorageService storageService) {
        this.storageService = storageService;
    }

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

        return "index.html";
    }

    /**
     * Handles file uploads to the server. The files will be stored as
     * provided by the used {@link StorageService} of this controller.
     *
     * @param request The Http request containing the uploaded files
     *                as {@link Part}, which will be stored using the
     *                provided {@link #storageService}.
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public String handleUpload(final HttpServletRequest request) {
        // Get the session ID
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new StorageException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();
        String projectId = (String) session.getAttribute("projectId");

        // Clear any existing files for this session only
        storageService.deleteAllForSession(sessionId);

        try {
            for (Part part : request.getParts()) {
                storageService.store(part, sessionId);
            }
        } catch (IOException | ServletException e) {
            throw new StorageException("No files for upload selected.");
        }

        // Return the project ID so clients can see which project they're working with
        return "Upload successful for project: " + projectId;
    }

    /**
     * An ExceptionHandler for handling {@link StorageException}s.
     * If an exceptions occurs, this handler returns a string representation of
     * the message and cause.
     *
     * @param e The thrown exception.
     * @return The message and cause of the exception as String.
     */
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