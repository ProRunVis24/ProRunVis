package api.controller;

import api.service.TracingService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TracingService service;

    public TraceController(TracingService service) {
        this.service = service;
    }

    /**
     * POST /api/trace?instrumentId=<localId>
     * Runs the trace process for the given localId.
     */
    @PostMapping
<<<<<<< Updated upstream
    public String runTrace(@RequestParam String instrumentId,
                           HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();

        service.runTrace(instrumentId, sessionId);
        return instrumentId; // Return the localId for further processing.
=======
    public String runTrace(
            @RequestParam String instrumentId,
            HttpServletRequest request) {

        // Get session ID from the request
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();

        // Run the trace with both instrumentId and sessionId
        service.runTrace(instrumentId, sessionId);

        // Return the instrument ID for the next step
        return instrumentId;
>>>>>>> Stashed changes
    }
}