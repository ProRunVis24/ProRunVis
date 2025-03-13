// UploadController.java
package api.upload;

import api.upload.storage.StorageException;
import api.upload.storage.StorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
public class UploadController {
    private final StorageService storageService;

    @Autowired
    public UploadController(final StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * GET /?projectId=<projectId>
     * Hosts the landing page after ensuring the project directories are initialized.
     */
    @GetMapping("/")
    public String getIndex(HttpServletResponse response,
                           @RequestParam(required = false) String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return "redirect:/new-project";
        }
        storageService.initProject(projectId);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        return "index.html";
    }

    /**
     * POST /api/upload
     * Handles file uploads to a project-specific folder.
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public String handleUpload(final HttpServletRequest request) {
        String projectId = request.getParameter("projectId");
        if (projectId == null || projectId.isEmpty()) {
            throw new StorageException("No project ID provided. Please specify a project ID.");
        }
        storageService.deleteAllForProject(projectId);
        try {
            for (Part part : request.getParts()) {
                // Skip the part if its name is "projectId"
                if ("projectId".equals(part.getName())) {
                    continue;
                }
                storageService.store(part, projectId);
            }
        } catch (IOException | ServletException e) {
            throw new StorageException("No files for upload selected.", e);
        }
        return "Upload successful for project: " + projectId;
    }

    @ExceptionHandler(StorageException.class)
    @ResponseBody
    public String handleException(final StorageException e) {
        String error = e.getMessage() + "\n";
        if (e.getCause() != null) {
            error += "\n" + e.getCause() + "\n";
        }
        return error;
    }
}