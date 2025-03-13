package api.controller;

import api.functionality.StaticMethodExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class StaticMethodController {

    private final StaticMethodExtractorService extractorService;

    @Autowired
    public StaticMethodController(StaticMethodExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    // Expose an endpoint that returns the static methods JSON
    @GetMapping("/api/static-methods")
    public String getStaticMethods(@RequestParam String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("No project ID provided. Please specify a project ID.");
        }

        return extractorService.toJSON(projectId);
    }
}