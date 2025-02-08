package api.service;

import api.service.JBMCFlattenService.FlattenedAssignment;
import prorunvis.trace.TraceNode;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

@Service
public class JBMCFlattenService {

    private static final Logger logger = Logger.getLogger(JBMCFlattenService.class.getName());

    /**
     * Flattens the given list of TraceNodes into a list of FlattenedAssignment objects.
     *
     * @param nodeList the list of TraceNodes to flatten
     * @return a list of FlattenedAssignment objects
     */
    public List<FlattenedAssignment> flatten(List<TraceNode> nodeList) {
        logger.info("Starting flattening of " + nodeList.size() + " trace nodes.");
        List<FlattenedAssignment> assignments = new ArrayList<>();
        try {
            // Example logic (adjust according to your actual flattening code):
            for (TraceNode node : nodeList) {
                if (node.getJbmcValues() != null) {
                    node.getJbmcValues().forEach((varName, varValues) -> {
                        for (TraceNode.VarValue val : varValues) {
                            assignments.add(new FlattenedAssignment(node.getTraceID(), varName, val.getIteration(), val.getValue()));
                        }
                    });
                }
            }
            logger.info("Flattening complete. Total assignments: " + assignments.size());
        } catch (Exception e) {
            logger.severe("Error during flattening: " + e.getMessage());
            throw e;
        }
        return assignments;
    }

    /**
     * Inner class to represent a flattened JBMC variable assignment.
     */
    public static class FlattenedAssignment {
        private String nodeTraceId;
        private String variableName;
        private int iteration;
        private String value;

        public FlattenedAssignment(String nodeTraceId, String variableName, int iteration, String value) {
            this.nodeTraceId = nodeTraceId;
            this.variableName = variableName;
            this.iteration = iteration;
            this.value = value;
        }

        // Getters and setters...

        public String getNodeTraceId() {
            return nodeTraceId;
        }

        public void setNodeTraceId(String nodeTraceId) {
            this.nodeTraceId = nodeTraceId;
        }

        public String getVariableName() {
            return variableName;
        }

        public void setVariableName(String variableName) {
            this.variableName = variableName;
        }

        public int getIteration() {
            return iteration;
        }

        public void setIteration(int iteration) {
            this.iteration = iteration;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}