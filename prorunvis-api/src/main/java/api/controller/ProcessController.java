package api.controller;

import api.service.ProcessingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    private final ProcessingService processingService;

    public ProcessController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * POST /api/process?traceId=<shortId>&projectId=<projectId>
     *
     * We'll read local_storage/project-<projectId>/<shortId>/Trace.tr,
     * produce processedTrace.json, store it in the same folder,
     * and return the shortId (or path) for the next step.
     */
    @PostMapping
    public String processTrace(
            @RequestParam String traceId,
            @RequestParam String projectId) {

        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("No project ID provided. Please specify a project ID.");
        }

        // run the processing with project ID
        processingService.processTrace(traceId, projectId);

        // return the same short ID for the front end
        // so the front end can do a GET /api/visualize/<traceId> if desired
        return traceId;
    }
}