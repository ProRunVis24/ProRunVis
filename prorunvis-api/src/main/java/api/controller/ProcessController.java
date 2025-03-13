package api.controller;

import api.service.ProcessingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    private final ProcessingService processingService;

    public ProcessController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * POST /api/process?traceId=<shortId>
     *
     * We'll read local_storage/session-<sessionId>/<shortId>/Trace.tr,
     * produce processedTrace.json, store it in the same folder,
     * and return the shortId (or path) for the next step.
     */
    @PostMapping
    public String processTrace(@RequestParam String traceId,
                               HttpServletRequest request) {
        // Get the session ID
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("No active session found. Please refresh the page.");
        }
        String sessionId = session.getId();

        // run the processing with session ID
        processingService.processTrace(traceId, sessionId);

        // return the same short ID for the front end
        // so the front end can do a GET /api/visualize/<traceId> if desired
        return traceId;
    }
}