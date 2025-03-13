package api.controller;

import api.service.JBMCService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/jbmc")
public class JBMCController {

    private final JBMCService jbmcService;

    public JBMCController(JBMCService jbmcService) {
        this.jbmcService = jbmcService;
    }

    /**
     * POST /api/jbmc/run?instrumentId=<localId>&methodSignature=<sig>&unwind=5&maxArray=5
     * This triggers JBMC to run using the compiled code corresponding to that localId.
     */
    @PostMapping("/run")
    public ResponseEntity<String> runJBMC(@RequestParam("instrumentId") String instrumentId,
                                          @RequestParam("methodSignature") String methodSignature,
                                          @RequestParam(name = "unwind", defaultValue = "5") int unwind,
                                          @RequestParam(name = "maxArray", defaultValue = "5") int maxArray,
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

        jbmcService.runJBMC(instrumentId, methodSignature, unwind, maxArray, sessionId);
<<<<<<< Updated upstream
        String msg = "JBMC run complete. JSON stored in local_storage/session-"
                + sessionId + "/" + instrumentId + "/jbmcOutput.json";
        return ResponseEntity.ok(msg);
=======
        return ResponseEntity.ok("JBMC run complete. JSON stored in local_storage/session-" + sessionId + "/" + instrumentId + "/jbmcOutput.json");
>>>>>>> Stashed changes
    }

    /**
     * GET /api/jbmc/result/<instrumentId>
     * Returns the JBMC JSON result stored under the given localId.
     */
    @GetMapping(value = "/result/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getJBMCResult(@PathVariable String instrumentId,
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

        String jbmcJson = jbmcService.getJBMCOutput(instrumentId, sessionId);
        return ResponseEntity.ok(jbmcJson);
    }
}