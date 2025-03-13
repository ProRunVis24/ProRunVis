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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProcessingService {

    private static final String LOCAL_STORAGE_DIR = "resources/local_storage";

    // Store the processed nodes per session
    private final Map<String, List<TraceNode>> sessionProcessedNodes = new ConcurrentHashMap<>();

    public void processTrace(String traceId, String sessionId) {
        System.out.println("[processTrace] Starting process for traceId = " + traceId + ", sessionId = " + sessionId);

        // 1) local_storage/session-<sessionId>/<traceId> must exist
        File localIdFolder = new File(LOCAL_STORAGE_DIR + "/session-" + sessionId, traceId);
        if (!localIdFolder.exists() || !localIdFolder.isDirectory()) {
            throw new RuntimeException("Local ID folder does not exist: " + localIdFolder.getAbsolutePath() + " for session: " + sessionId);
        }

        // 2) local_storage/session-<sessionId>/<traceId>/Trace.tr must exist
        File traceFile = new File(localIdFolder, "Trace.tr");
        if (!traceFile.exists()) {
            throw new RuntimeException("Trace file not found: " + traceFile.getAbsolutePath() + " for session: " + sessionId);
        }

        // 3) Re-parse code from session-specific input directory
        Path codeRoot = Paths.get("resources/in/session-" + sessionId);
        System.out.println("[processTrace] Parsing project from: " + codeRoot.toAbsolutePath() + " for session: " + sessionId);
        ProjectRoot projectRoot = Util.parseProject(codeRoot.toFile());
        List<CompilationUnit> cus = Util.getCUs(projectRoot);

        // 4) Build the map of AST nodes
        Map<Integer, Node> map = new HashMap<>();
        for (CompilationUnit cu : cus) {
            // Preprocess & Instrument
            prorunvis.preprocess.Preprocessor.run(cu);
            prorunvis.instrument.Instrumenter.run(cu, map);
        }

        // 5) Construct the TraceProcessor
        TraceProcessor processor = new TraceProcessor(map, traceFile.getAbsolutePath(), codeRoot);
        try {
            processor.start();
        } catch (Exception e) {
            throw new RuntimeException("Processing failed for session: " + sessionId + ", error: " + e.getMessage(), e);
        }

        // 6) Get trace nodes
        List<TraceNode> nodeList = processor.getNodeList();

        // Store in session-specific memory
        sessionProcessedNodes.put(sessionId, nodeList);

        System.out.println("[processTrace] Found " + nodeList.size() + " trace nodes for session: " + sessionId);

        // 7) Merge JBMC data (including bridging variable names)
        mergeJBMCValues(traceId, nodeList, sessionId);

        // 8) Write processedTrace.json to session-specific folder
        File outputJson = new File(localIdFolder, "processedTrace.json");
        String json = new Gson().toJson(nodeList);
        try (FileOutputStream fos = new FileOutputStream(outputJson)) {
            fos.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write processedTrace.json at: " + outputJson.getAbsolutePath() +
                    " for session: " + sessionId, e);
        }
        System.out.println("[processTrace] Completed. JSON at: " + outputJson.getAbsolutePath() + " for session: " + sessionId);
    }

    // For backward compatibility
    public void processTrace(String traceId) {
        processTrace(traceId, "default");
    }

    /**
     * Returns the in-memory list of TraceNodes from the last processing step for a specific session.
     */
    public List<TraceNode> getLastProcessedNodes(String sessionId) {
        return sessionProcessedNodes.getOrDefault(sessionId, Collections.emptyList());
    }

    // For backward compatibility
    public List<TraceNode> getLastProcessedNodes() {
        return getLastProcessedNodes("default");
    }

    /**
     * Reads jbmcOutput.json from the session-specific folder and merges JBMC values
     * into the trace nodes.
     */
    public void mergeJBMCValues(String traceId, List<TraceNode> nodeList, String sessionId) {
        File localFolder = new File(LOCAL_STORAGE_DIR + "/session-" + sessionId, traceId);
        File jbmcFile = new File(localFolder, "jbmcOutput.json");
        if (!jbmcFile.exists()) {
            System.out.println("[mergeJBMCValues] No jbmcOutput.json for " + traceId + " in session: " + sessionId + ". Skipping.");
            return;
        }

        String jbmcJson;
        try {
            jbmcJson = java.nio.file.Files.readString(jbmcFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Could not read jbmcOutput.json for " + traceId + " in session: " + sessionId, e);
        }

        List<JBMCParser.VarAssignment> assignments = JBMCParser.parseVariableAssignments(jbmcJson);
        System.out.println("[mergeJBMCValues] Found " + assignments.size() + " JBMC assignments for session: " + sessionId);

        // Build a map of (file, line) -> declared variable name(s) from session-specific user code
        VariableNameMapper varMapper = new VariableNameMapper();
        try {
            varMapper.buildVarNameMapping(Paths.get("resources/in/session-" + sessionId));
        } catch (IOException e) {
            throw new RuntimeException("Failed building variable name map from user code for session: " + sessionId, e);
        }
        Map<String, Map<Integer, List<String>>> varDeclMap = varMapper.getVarDeclMap();
        System.out.println("[mergeJBMCValues] Built variable decl map with " + varDeclMap.size() +
                " files for session: " + sessionId);

        int totalMatches = 0;

        for (JBMCParser.VarAssignment va : assignments) {
            // Map JBMC variable name to the declared source-level name, if available
            String realVarName = va.variableName;
            if (va.file != null && !va.file.isEmpty() && va.line > 0) {
                String fileKey = new File(va.file).getName();  // Normalize file name
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

            System.out.println("[mergeJBMCValues] For session: " + sessionId +
                    ", mapping assignment: JBMC variable '" + va.variableName +
                    "' mapped to real variable '" + realVarName + "'");

            boolean foundTraceNode = false;
            for (TraceNode node : nodeList) {
                if (node.getLink() == null) continue;
                String nodeFilePath = node.getLink().getFilepath();
                // Compare normalized file names
                if (nodeFilePath != null && nodeFilePath.endsWith(new File(va.file).getName())) {
                    boolean matchedRange = node.getRanges().stream().anyMatch(r ->
                            (r.begin.line <= va.line && va.line <= r.end.line));
                    if (matchedRange) {
                        if (node.getJbmcValues() == null) {
                            node.setJbmcValues(new HashMap<>());
                        }
                        node.getJbmcValues().computeIfAbsent(realVarName, k -> new ArrayList<>());

                        // Prefer the trace node's own iteration if set; otherwise, fall back
                        int iterationValue = (node.getIteration() != null) ? node.getIteration() : totalMatches + 1;
                        // Use the trace node's id
                        String currentTraceId = node.getTraceID();
                        TraceNode.VarValue varVal = new TraceNode.VarValue(currentTraceId, iterationValue, va.value);
                        node.getJbmcValues().get(realVarName).add(varVal);
                        System.out.println("[mergeJBMCValues] For session: " + sessionId +
                                ", added VarValue(traceId=" + currentTraceId +
                                ", iteration=" + iterationValue + ", value='" + va.value +
                                "') for variable '" + realVarName + "' in trace node ID='" + node.getTraceID() + "'.");
                        foundTraceNode = true;
                        totalMatches++;
                        break; // Attach to the first matching TraceNode
                    }
                }
            }
            if (!foundTraceNode) {
                System.out.println("For session: " + sessionId +
                        ", no TraceNode found for JBMC assignment at " + va.file +
                        ":" + va.line + ", variable=" + va.variableName +
                        " (mapped as '" + realVarName + "')");
            }
        }

        System.out.println("[mergeJBMCValues] For session: " + sessionId +
                ", merged JBMC variable values into " + totalMatches + " trace nodes.");
    }

    // For backward compatibility
    public void mergeJBMCValues(String traceId, List<TraceNode> nodeList) {
        mergeJBMCValues(traceId, nodeList, "default");
    }
}