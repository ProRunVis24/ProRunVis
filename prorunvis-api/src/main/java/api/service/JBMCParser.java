package api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example utility to parse JBMC "trace" steps for variable assignments.
 */
public class JBMCParser {

    /**
     * Return a list of (variableName, value) pairs from the JBMC JSON "trace.steps"
     */
    public static List<VarAssignment> parseVariableAssignments(String jbmcJson) {
        List<VarAssignment> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jbmcJson);
            // Typically "result" array, each with "trace.steps"
            if (root.has("result") && root.get("result").isArray()) {
                for (JsonNode r : root.get("result")) {
                    if (r.has("trace")) {
                        JsonNode steps = r.get("trace").get("steps");
                        if (steps != null && steps.isArray()) {
                            for (JsonNode step : steps) {
                                if (step.has("stepType") && "assignment".equals(step.get("stepType").asText())) {
                                    String lhs = step.has("lhs") ? step.get("lhs").asText() : "<no_var>";
                                    String val = step.has("value") ? step.get("value").asText() : "<no_val>";
                                    result.add(new VarAssignment(lhs, val));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JBMC JSON", e);
        }

        return result;
    }

    public static class VarAssignment {
        public String variableName;
        public String value;

        public VarAssignment(String variableName, String value) {
            this.variableName = variableName;
            this.value = value;
        }
    }
}