package api.controller;

import api.service.VisualizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

<<<<<<< Updated upstream
=======
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Returns JSON from the processedTrace.json file in local_storage/session-<sessionId>/<localId>.
 */
>>>>>>> Stashed changes
@RestController
@RequestMapping("/api/visualize")
public class VisualizationController {

    private final VisualizationService service;
    private final ObjectMapper objectMapper;

    public VisualizationController(VisualizationService service) {
        this.service = service;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * GET /api/visualize/{localId}
     * Reads processedTrace.json from resources/local_storage/session-<sessionId>/<localId>.
     */
    @GetMapping(value = "/{localId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getTraceJson(@PathVariable String localId,
                                               HttpServletRequest request) {
<<<<<<< Updated upstream
=======
        // Get the session ID
>>>>>>> Stashed changes
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.badRequest().body("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();

        String rawJson = service.getTraceJson(localId, sessionId);
        try {
            Object jsonObj = objectMapper.readValue(rawJson, Object.class);
            return ResponseEntity.ok(jsonObj);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse processedTrace.json: " + e.getMessage());
        }
    }
}