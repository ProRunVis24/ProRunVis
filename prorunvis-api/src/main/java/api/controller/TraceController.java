package api.controller;

import api.service.TracingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TracingService service;

    public TraceController(TracingService service) {
        this.service = service;
    }

    /**
     * POST /api/trace?instrumentId=<localId>&projectId=<projectId>
     * Runs the trace process for the given localId and projectId.
     */
    @PostMapping
    public String runTrace(
            @RequestParam String instrumentId,
            @RequestParam String projectId) {

        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("No project ID provided. Please specify a project ID.");
        }

        // Run the trace with both instrumentId and projectId
        service.runTrace(instrumentId, projectId);

        // Return the instrument ID for the next step
        return instrumentId;
    }
}