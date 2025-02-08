package api.service;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.utils.ProjectRoot;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import prorunvis.trace.TraceNode;
import prorunvis.trace.process.TraceProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ProcessingService {

    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";
    // NEW: keep the last processed node list in memory
    private List<TraceNode> lastProcessedNodes;

    public void processTrace(String traceId) {


        System.out.println("[processTrace] Starting process for traceId = " + traceId);

        // 1) local_storage/<traceId> must exist
        File localIdFolder = new File(LOCAL_STORAGE_DIR, traceId);
        if (!localIdFolder.exists() || !localIdFolder.isDirectory()) {
            throw new RuntimeException("Local ID folder does not exist: " + localIdFolder.getAbsolutePath());
        }

        // 2) local_storage/<traceId>/Trace.tr must exist
        File traceFile = new File(localIdFolder, "Trace.tr");
        if (!traceFile.exists()) {
            throw new RuntimeException("Trace file not found: " + traceFile.getAbsolutePath());
        }

        // 3) Re-parse code from resources/in
        Path codeRoot = Paths.get("resources/in");
        System.out.println("[processTrace] Parsing project from: " + codeRoot.toAbsolutePath());
        ProjectRoot projectRoot = Util.parseProject(codeRoot.toFile());
        List<CompilationUnit> cus = Util.getCUs(projectRoot);

        // 4) Build the map of AST nodes
        Map<Integer, Node> map = new HashMap<>();
        for (CompilationUnit cu : cus) {
            // Preprocess & Instrument
            prorunvis.preprocess.Preprocessor.run(cu);
            prorunvis.instrument.Instrumenter.run(cu, map);
        }// store in memory
        // 5) Construct the TraceProcessor
        TraceProcessor processor = new TraceProcessor(map, traceFile.getAbsolutePath(), codeRoot);
        List<TraceNode> nodeList = processor.getNodeList();
        this.lastProcessedNodes = nodeList;
        try {
            processor.start();
        } catch (Exception e) {
            throw new RuntimeException("Processing failed: " + e.getMessage(), e);
        }

        // 6) Final trace nodes

        System.out.println("[processTrace] Found " + nodeList.size() + " trace nodes.");

        // 7) Merge JBMC data (including bridging variable names)
        mergeJBMCValues(traceId, nodeList);

        // 8) Write processedTrace.json
        File outputJson = new File(localIdFolder, "processedTrace.json");
        String json = new Gson().toJson(nodeList);
        try (FileOutputStream fos = new FileOutputStream(outputJson)) {
            fos.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write processedTrace.json at: " + outputJson.getAbsolutePath(), e);
        }
        System.out.println("[processTrace] Completed. JSON at: " + outputJson.getAbsolutePath());
    }
    /**
     * Returns the in-memory list of TraceNodes from the last processing step.
     * If no processing has occurred, or if you want to track them *by traceId*,
     * you can store a Map<String,List<TraceNode>> instead.
     */
    public List<TraceNode> getLastProcessedNodes() {
        return this.lastProcessedNodes;
    }

    /**
     * Reads jbmcOutput.json (if present) for the given traceId, parses variable
     * assignments, THEN attempts to map JBMC's "lhs" to the original
     * variable name by scanning the user source (resources/in).
     *
     * Finally, we attach that data to the TraceNode's getJbmcValues().
     */
    public void mergeJBMCValues(String traceId, List<TraceNode> nodeList) {
        File localFolder = new File(LOCAL_STORAGE_DIR, traceId);
        File jbmcFile = new File(localFolder, "jbmcOutput.json");
        if (!jbmcFile.exists()) {
            System.out.println("[mergeJBMCValues] No jbmcOutput.json for " + traceId + ". Skipping.");
            return;
        }

        String jbmcJson;
        try {
            jbmcJson = java.nio.file.Files.readString(jbmcFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Could not read jbmcOutput.json for " + traceId, e);
        }

        List<JBMCParser.VarAssignment> assignments = JBMCParser.parseVariableAssignments(jbmcJson);
        System.out.println("[mergeJBMCValues] Found " + assignments.size() + " JBMC assignments.");

        // Build a map of (file, line) -> declared variable name(s) from user code.
        VariableNameMapper varMapper = new VariableNameMapper();
        try {
            varMapper.buildVarNameMapping(Paths.get("resources/in"));
        } catch (IOException e) {
            throw new RuntimeException("Failed building variable name map from user code", e);
        }
        Map<String, Map<Integer, List<String>>> varDeclMap = varMapper.getVarDeclMap();
        System.out.println("[mergeJBMCValues] Built variable decl map with " + varDeclMap.size() + " files.");

        int totalMatches = 0;

        for (JBMCParser.VarAssignment va : assignments) {
            // Map JBMC variable name to the declared source-level name, if available.
            String realVarName = va.variableName;
            if (va.file != null && !va.file.isEmpty() && va.line > 0) {
                String fileKey = new File(va.file).getName();  // Normalize file name.
                if (varDeclMap.containsKey(fileKey)) {
                    Map<Integer, List<String>> lineMap = varDeclMap.get(fileKey);
                    if (lineMap.containsKey(va.line)) {
                        List<String> declaredVars = lineMap.get(va.line);
                        if (!declaredVars.isEmpty()) {
                            realVarName = declaredVars.get(0);
                        }
                    }
                }
            }

            System.out.println("[mergeJBMCValues] Mapping assignment: JBMC variable '"
                    + va.variableName + "' mapped to real variable '" + realVarName + "'");

            boolean foundTraceNode = false;
            for (TraceNode node : nodeList) {
                if (node.getLink() == null) continue;
                String nodeFilePath = node.getLink().getFilepath();
                // Compare normalized file names.
                if (nodeFilePath != null && nodeFilePath.endsWith(new File(va.file).getName())) {
                    boolean matchedRange = node.getRanges().stream().anyMatch(r ->
                            (r.begin.line <= va.line && va.line <= r.end.line));
                    if (matchedRange) {
                        if (node.getJbmcValues() == null) {
                            node.setJbmcValues(new HashMap<>());
                        }
                        node.getJbmcValues().computeIfAbsent(realVarName, k -> new ArrayList<>());

                        // Prefer the trace node's own iteration if set; otherwise, fall back.
                        int iterationValue = (node.getIteration() != null) ? node.getIteration() : totalMatches + 1;
                        // Use the trace node's id.
                        String currentTraceId = node.getTraceID();
                        TraceNode.VarValue varVal = new TraceNode.VarValue(currentTraceId, iterationValue, va.value);
                        node.getJbmcValues().get(realVarName).add(varVal);
                        System.out.println("[mergeJBMCValues] Added VarValue(traceId=" + currentTraceId +
                                ", iteration=" + iterationValue + ", value='" + va.value +
                                "') for variable '" + realVarName + "' in trace node ID='" + node.getTraceID() + "'.");
                        foundTraceNode = true;
                        totalMatches++;
                        break; // Attach to the first matching TraceNode.
                    }
                }
            }
            if (!foundTraceNode) {
                System.out.println("No TraceNode found for JBMC assignment at " + va.file +
                        ":" + va.line + ", variable=" + va.variableName +
                        " (mapped as '" + realVarName + "')");
            }
        }

        System.out.println("[mergeJBMCValues] Merged JBMC variable values into " + totalMatches + " trace nodes.");
    }

}