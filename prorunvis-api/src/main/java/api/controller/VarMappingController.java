package api.controller;

import api.service.VariableNameMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/varMapping")
public class VarMappingController {

    /**
     * Given a file name and line number, returns a list of variable names
     * declared on that line. For example, if the file "MyClass.java" has two
     * variables declared on line 15, this endpoint returns them.
     *
     * URL example: GET /api/varMapping?file=MyClass.java&line=15
     */
    @GetMapping
    public ResponseEntity<List<String>> getVariables(
            @RequestParam String file,
            @RequestParam int line,
            HttpServletRequest request) {

        // Get the session ID
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.badRequest()
                    .body(Collections.emptyList());
        }
        String sessionId = session.getId();

        VariableNameMapper mapper = new VariableNameMapper();
        try {
            // Build the mapping from the session-specific source directory
            mapper.buildVarNameMapping(Paths.get("resources/in/session-" + sessionId));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }

        Map<String, Map<Integer, List<String>>> varDeclMap = mapper.getVarDeclMap();
        // Extract only the simple file name
        String simpleFile = new File(file).getName();
        Map<Integer, List<String>> fileMapping = varDeclMap.get(simpleFile);
        List<String> variables = (fileMapping != null) ? fileMapping.get(line) : null;
        if (variables == null) {
            variables = Collections.emptyList();
        }
        return ResponseEntity.ok(variables);
    }
}