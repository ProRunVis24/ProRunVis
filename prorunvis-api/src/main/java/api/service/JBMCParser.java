package api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JBMCParser {

    public static class VarAssignment {
        public final String variableName;
        public final String value;
        public final String file;
        public final int line;
        public final int iteration;

        public VarAssignment(String variableName, String value, String file, int line, int iteration) {
            this.variableName = variableName;
            this.value = value;
            this.file = file;
            this.line = line;
            this.iteration = iteration;
        }

        @Override
        public String toString() {
            return "VarAssignment{" +
                    "variableName='" + variableName + '\'' +
                    ", value='" + value + '\'' +
                    ", file='" + file + '\'' +
                    ", line=" + line +
                    ", iteration=" + iteration +
                    '}';
        }
    }

    public static List<VarAssignment> parseVariableAssignments(String jbmcJson) {
        List<VarAssignment> assignments = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jbmcJson);

            if (root.isArray()) {
                for (JsonNode element : root) {
                    if (element.has("result")) {
                        JsonNode resultArray = element.get("result");
                        if (resultArray.isArray()) {
                            for (JsonNode res : resultArray) {
                                if (res.has("trace")) {
                                    JsonNode traceArray = res.get("trace");
                                    if (traceArray.isArray()) {
                                        for (JsonNode traceStep : traceArray) {
                                            if (traceStep.has("stepType") &&
                                                    "assignment".equals(traceStep.get("stepType").asText())) {
                                                String variableName = "unknown";
                                                if (traceStep.has("variableName")) {
                                                    variableName = traceStep.get("variableName").asText();
                                                } else if (traceStep.has("lhs")) {
                                                    variableName = traceStep.get("lhs").asText();
                                                }

                                                String value = "";
                                                if (traceStep.has("value")) {
                                                    JsonNode valueNode = traceStep.get("value");
                                                    if (valueNode.isObject() && valueNode.has("data")) {
                                                        value = valueNode.get("data").asText();
                                                    } else {
                                                        value = valueNode.asText();
                                                    }
                                                }

                                                String file = "unknown";
                                                int line = -1;
                                                if (traceStep.has("sourceLocation")) {
                                                    JsonNode sourceLoc = traceStep.get("sourceLocation");
                                                    if (sourceLoc.has("file")) {
                                                        file = sourceLoc.get("file").asText();
                                                    }
                                                    if (sourceLoc.has("line")) {
                                                        line = sourceLoc.get("line").asInt(-1);
                                                    }
                                                }

                                                int iteration = -1;
                                                if (traceStep.has("iteration")) {
                                                    iteration = traceStep.get("iteration").asInt(-1);
                                                }

                                                assignments.add(new VarAssignment(variableName, value, file, line, iteration));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JBMC JSON", e);
        }
        return assignments;
    }
}
