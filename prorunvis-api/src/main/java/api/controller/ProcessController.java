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
<<<<<<< Updated upstream
     * POST /api/process?traceId=<localId>
     * Processes the trace file located in resources/local_storage/session-<sessionId>/<localId>/Trace.tr,
     * writes processedTrace.json, and returns the localId.
=======
     * POST /api/process?traceId=<shortId>
     *
     * We'll read local_storage/session-<sessionId>/<shortId>/Trace.tr,
     * produce processedTrace.json, store it in the same folder,
     * and return the shortId (or path) for the next step.
>>>>>>> Stashed changes
     */
    @PostMapping
    public String processTrace(@RequestParam String traceId,
                               HttpServletRequest request) {
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

        // run the processing with session ID
        processingService.processTrace(traceId, sessionId);
>>>>>>> Stashed changes

        processingService.processTrace(traceId, sessionId);
        return traceId;
    }
}