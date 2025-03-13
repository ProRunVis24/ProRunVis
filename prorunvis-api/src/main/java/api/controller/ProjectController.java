package api.controller;

import api.upload.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.UUID;

@Controller
public class ProjectController {

    private final StorageService storageService;

    @Autowired
    public ProjectController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Creates a brand new session and redirects to the home page.
     * Use this to force a new project in a new tab.
     */
    @GetMapping("/new-project")
    public String newProject(HttpServletRequest request, HttpServletResponse response) {
        // Force invalidation of any existing session
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        // Create a new session with a unique ID
        HttpSession newSession = request.getSession(true);
        String sessionId = newSession.getId();

        // Create a unique project ID and store it in the session
        String projectId = UUID.randomUUID().toString().substring(0, 8);
        newSession.setAttribute("projectId", projectId);

        // Initialize storage for this new session
        storageService.initSession(sessionId);

        // Disable caching to ensure we always get a fresh page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        System.out.println("Created new project with session ID: " + sessionId + " and project ID: " + projectId);

        return "redirect:/?newSession=true";
    }

    /**
     * Debug endpoint to check session information.
     */
    @GetMapping("/debug/session")
    @ResponseBody
    public String debugSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "No active session";
        }

        StringBuilder debug = new StringBuilder();
        debug.append("Session ID: ").append(session.getId()).append("<br>");
        debug.append("Creation Time: ").append(new Date(session.getCreationTime())).append("<br>");
        debug.append("Project ID: ").append(session.getAttribute("projectId")).append("<br>");
        debug.append("Session Max Inactive Interval: ").append(session.getMaxInactiveInterval()).append(" seconds<br>");

        return debug.toString();
    }
}