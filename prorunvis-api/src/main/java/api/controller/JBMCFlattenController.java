package api.controller;

import api.service.JBMCFlattenService;
import api.service.ProcessingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/jbmc/flatten")
public class JBMCFlattenController {

    private static final Logger logger = Logger.getLogger(JBMCFlattenController.class.getName());

    private final ProcessingService processingService;
    private final JBMCFlattenService flattenService;

    public JBMCFlattenController(ProcessingService processingService,
                                 JBMCFlattenService flattenService) {
        this.processingService = processingService;
        this.flattenService = flattenService;
    }

    /**
     * GET /api/jbmc/flatten
     * Returns an array of FlattenedAssignment objects in JSON.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFlattenedAssignments() {
        logger.info("Received request for flattened JBMC assignments.");

        // 1) Grab the last processed nodes from memory
        var nodeList = processingService.getLastProcessedNodes();
        if (nodeList == null || nodeList.isEmpty()) {
            logger.warning("No in-memory trace data found. Did you run /api/process yet?");
            return ResponseEntity.badRequest().body("No in-memory trace data found. Did you run /api/process yet?");
        }
        logger.info("Retrieved " + nodeList.size() + " processed trace nodes from memory.");

        // 2) Flatten them
        List<JBMCFlattenService.FlattenedAssignment> flattened;
        try {
            flattened = flattenService.flatten(nodeList);
            logger.info("Successfully flattened assignments. Total flattened assignments: " + flattened.size());
        } catch (Exception e) {
            logger.severe("Error flattening JBMC assignments: " + e.getMessage());
            return ResponseEntity.status(500).body("Error flattening JBMC assignments: " + e.getMessage());
        }

        // 3) Return as JSON
        logger.info("Returning flattened JBMC assignments as JSON.");
        return ResponseEntity.ok(flattened);
    }
}