// InstrumentController.java
package api.controller;

import api.service.InstrumentationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/instrument")
public class InstrumentController {
    private final InstrumentationService service;

    public InstrumentController(InstrumentationService service) {
        this.service = service;
    }

    /**
     * POST /api/instrument?projectName=<name>&inputDir=[optional]&projectId=<projectId>
     * Instruments a project using project-based file storage.
     */
    @PostMapping
    public String instrumentProject(
            @RequestParam String projectName,
            @RequestParam(required = false) String inputDir,
            @RequestParam String projectId
    ) {
        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("No project ID provided. Please specify a project ID.");
        }
        if (inputDir == null || inputDir.isEmpty()) {
            inputDir = "resources/in/project-" + projectId;
        }
        String localId = UUID.randomUUID().toString();
        service.instrumentProject(projectName, inputDir, localId, projectId);
        return localId;
    }
}