package api.controller;

import api.functionality.StaticMethodExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
public class StaticMethodController {

    private final StaticMethodExtractorService extractorService;

    @Autowired
    public StaticMethodController(StaticMethodExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    @GetMapping("/api/static-methods")
    public String getStaticMethods(HttpServletRequest request) {
<<<<<<< Updated upstream
=======
        // Get the session ID
>>>>>>> Stashed changes
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
        return extractorService.toJSON(sessionId);
    }
}