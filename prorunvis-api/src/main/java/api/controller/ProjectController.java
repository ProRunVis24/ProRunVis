package api.controller;

import api.upload.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.UUID;

@Controller
public class ProjectController {

    private final StorageService storageService;

    @Autowired
    public ProjectController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Creates a brand new project and redirects to the home page.
     * Use this to force a new project in a new tab.
     */
    @GetMapping("/new-project")
    public String newProject(HttpServletResponse response) {
        // Create a unique project ID
        String projectId = UUID.randomUUID().toString().substring(0, 8);

        // Initialize storage for this new project
        storageService.initProject(projectId);

        // Disable caching to ensure we always get a fresh page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        System.out.println("Created new project with project ID: " + projectId);

        return "redirect:/?projectId=" + projectId;
    }

    /**
     * Debug endpoint to check project information.
     */
    @GetMapping("/debug/project")
    @ResponseBody
    public String debugProject(@RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return "No project ID provided";
        }

        StringBuilder debug = new StringBuilder();
        debug.append("Project ID: ").append(projectId).append("<br>");
        debug.append("Input Directory: ").append(storageService.getProjectInLocation(projectId)).append("<br>");
        debug.append("Output Directory: ").append(storageService.getProjectOutLocation(projectId)).append("<br>");
        debug.append("Local Storage: ").append(storageService.getProjectLocalStorageLocation(projectId)).append("<br>");

        return debug.toString();
    }
}