package api.controller;

import api.service.JBMCService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jbmc")
public class JBMCController {

    private final JBMCService jbmcService;

    public JBMCController(JBMCService jbmcService) {
        this.jbmcService = jbmcService;
    }

    /**
     * POST /api/jbmc/run?instrumentId=<localId>&methodSignature=<sig>&unwind=5&maxArray=5&projectId=<projectId>
     * This triggers JBMC to run for that localId's compiled code.
     */
    @PostMapping("/run")
    public ResponseEntity<String> runJBMC(
            @RequestParam("instrumentId") String instrumentId,
            @RequestParam("methodSignature") String methodSignature,
            @RequestParam(name = "unwind", defaultValue = "5") int unwind,
            @RequestParam(name = "maxArray", defaultValue = "5") int maxArray,
            @RequestParam String projectId) {

        if (projectId == null || projectId.isEmpty()) {
            return ResponseEntity.badRequest().body("No project ID provided. Please specify a project ID.");
        }

        jbmcService.runJBMC(instrumentId, methodSignature, unwind, maxArray, projectId);
        return ResponseEntity.ok("JBMC run complete. JSON stored in local_storage/project-" + projectId + "/" + instrumentId + "/jbmcOutput.json");
    }

    /**
     * GET /api/jbmc/result/<instrumentId>?projectId=<projectId>
     * Returns the raw JSON from JBMC's last run for that localId.
     */
    @GetMapping(value = "/result/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getJBMCResult(
            @PathVariable String instrumentId,
            @RequestParam String projectId) {

        if (projectId == null || projectId.isEmpty()) {
            return ResponseEntity.badRequest().body("No project ID provided. Please specify a project ID.");
        }

        String jbmcJson = jbmcService.getJBMCOutput(instrumentId, projectId);
        return ResponseEntity.ok(jbmcJson);
    }
}