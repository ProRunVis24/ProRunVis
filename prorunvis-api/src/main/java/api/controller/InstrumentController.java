package api.controller;

import api.service.InstrumentationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

@RestController
@RequestMapping("/api/instrument")
public class InstrumentController {
    private final InstrumentationService service;

    public InstrumentController(InstrumentationService service) {
        this.service = service;
    }

    /**
<<<<<<< Updated upstream
     * POST /api/instrument
     * Instruments a project. Generates a new localId for this instrumentation job.
=======
     * Returns a String ID referencing a local folder
     * Adds session ID awareness
>>>>>>> Stashed changes
     */
    @PostMapping
    public String instrumentProject(
            @RequestParam String projectName,
            @RequestParam(required = false) String inputDir,
            HttpServletRequest request
    ) {
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
        // If inputDir is not provided, use the session-specific default.
        // If you want to instrument a single project folder, you can use:
        // "resources/in/session-<sessionId>/<localId>".
        // For now, if inputDir is null, we assume the user uploaded files into
        // resources/in/session-<sessionId> (which may contain multiple projects).
=======
        // If inputDir not provided, use session-specific default
>>>>>>> Stashed changes
        if (inputDir == null || inputDir.isEmpty()) {
            inputDir = "resources/in/session-" + sessionId;
        }

<<<<<<< Updated upstream
        // Generate a unique localId (project ID) for this instrumentation job.
        String localId = UUID.randomUUID().toString();

        service.instrumentProject(projectName, inputDir, localId, sessionId);
        return localId;
=======
        // Generate a unique ID for this instrumentation job
        String randomId = UUID.randomUUID().toString();

        // Instrument the code with session awareness
        service.instrumentProject(projectName, inputDir, randomId, sessionId);

        // Return that ID so the frontend can pass it to subsequent endpoints
        return randomId;
>>>>>>> Stashed changes
    }
}