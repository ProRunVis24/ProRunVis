package prorunvis.trace.process;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.*;
import prorunvis.trace.TraceNode;

import java.io.IOException;
import java.util.*;

/**
 * This class is used to convert a previously generated id-trace
 * to a type of tree representation.
 * The resulting tree consists of {@link TraceNode} objects in a
 * list with index-based references to parents and children.
 */
public class TraceProcessor {

    /**
     * A list containing all the trace nodes in the tree.
     */
    private final List<TraceNode> nodeList;

    /**
     * A map which contains the corresponding {@link Node} of
     * the AST for every trace id.
     */
    private final Map<Integer, Node> traceMap;

    /**
     * The current trace node, serves as a global save state
     * across multiple recursions.
     */
    private TraceNode current;

    /**
     * The corresponding AST node for current.
     */
    private Node nodeOfCurrent;

    /**
     * A scanner object used to convert the trace file
     * into single trace id's.
     */
    private final Scanner scanner;

    /**
     * A stack containing the trace id's in correct
     * order generated by {@link #scanner}.
     */
    private Stack<Integer> tokens;

    /**
     * A list that is used to track ranges of method-calls that
     * have already been added as caller for a trace node, so that
     * multiple calls to a method in the same scope can be correctly
     * associated with the respective call-statement.
     */
    private List<Range> methodCallRanges;


    /**
     * Constructs a TraceProcessor for the given parameters.
     *
     * @param trace         A map containing all the possible trace-id's
     *                      and their corresponding nodes in the AST.
     * @param traceFilePath A string representation of the path to
     *                      the trace file containing the actual
     *                      recorded trace.
     */
    public TraceProcessor(final Map<Integer, Node> trace, final String traceFilePath) {
        this.nodeList = new LinkedList<>();
        this.traceMap = trace;
        this.scanner = new Scanner(traceFilePath);
        this.methodCallRanges = new ArrayList<>();
    }

    /**
     * Start the processor by creating the token stack and
     * the root for the tree.
     *
     * @throws IOException If the scanner can not open
     *                     or correctly read the trace file.
     */
    public void start() throws IOException {

        //read tokens to stack
        try {
            tokens = scanner.readFile();
        } catch (IOException e) {
            throw new IOException("Could not read trace file.", e);
        }

        createRoot();
    }

    /**
     * Creates the root node for the tree, which has no parent
     * and one guaranteed child-node for the first id in the trace.
     */
    private void createRoot() {
        TraceNode root = new TraceNode(null, "root");
        nodeList.add(root);
        current = root;

        //add the first node as child to root
        createNewTraceNode();
    }

    /**
     * Process a child of the current node by determining if the next
     * code block is a child of current and if yes, create it.
     *
     * @return false if a child was created to indicate that the children
     * for current are not finished, true otherwise.
     */
    private boolean processChild() {

        if (tokens.empty()) {
            return true;
        }

        Node node = traceMap.get(tokens.peek());

        //check if the node is a method declaration or not
        if (node instanceof MethodDeclaration) {
            return !createMethodCallTraceNode();
        } else {

            Optional<Range> range = node.getRange();
            Optional<Range> currentRange = nodeOfCurrent.getRange();

            //check if the next traced node is located within the node
            //of current
            if (range.isPresent() && currentRange.isPresent()) {
                if (currentRange.get().strictlyContains(range.get())) {
                    //create the new trace node
                    createNewTraceNode();
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Creates a new TraceNode, which will be added as child to current.
     * At the end of this method, the node including all its children are
     * correctly set up without need for further processing.
     */
    private void createNewTraceNode() {
        //create a new node and remove the token from the stack
        int tokenValue = tokens.pop();
        String name = String.valueOf(tokenValue);
        int parentIndex = nodeList.indexOf(current);
        TraceNode traceNode = new TraceNode(parentIndex, name);

        //add the node to the list and it's index as child of current
        nodeList.add(traceNode);
        current.addChildIndex(nodeList.indexOf(traceNode));

        //save the current state
        current = traceNode;
        Node tempNodeOfCurrent = nodeOfCurrent;
        nodeOfCurrent = traceMap.get(tokenValue);
        List<Range> tempRanges = methodCallRanges;
        methodCallRanges = new ArrayList<>();

        //add children to the created node, while there are still tokens
        //on the stack and new nodes can be created

        fillRanges((getBlockStmt() == null) ? nodeOfCurrent.getChildNodes() : getBlockStmt().getChildNodes(), null);

        //if current node is a loop: calculate and set iteration
        if (nodeOfCurrent instanceof NodeWithBody<?>) {
            int iteration = 0;
            for (int i: nodeList.get(current.getParentIndex()).getChildrenIndices()) {
                if (nodeList.get(i).getTraceID().equals(current.getTraceID())) {
                    iteration++;
                }
            }
            current.setIteration(iteration);
        }

        //restore state
        current = nodeList.get(traceNode.getParentIndex());
        nodeOfCurrent = tempNodeOfCurrent;
        methodCallRanges = tempRanges;
    }


    /**
     * Create a new trace node explicitly for a method call. For that the method
     * first checks within what type of node the call is located to get the
     * correct {@link MethodCallExpr} and then creates the node with the correct
     * link and out-link for that expression.
     *
     * @return a boolean to indicate if current may have further children.
     * True if the node was created, false otherwise.
     */
    private boolean createMethodCallTraceNode() {
        MethodDeclaration node = (MethodDeclaration) traceMap.get(tokens.peek());
        SimpleName nameOfDeclaration = node.getName();
        SimpleName nameOfCall = null;
        MethodCallExpr callExpr = null;
        //call extracted method to get the surrounding block-statement
        //if one is present
        BlockStmt block = getBlockStmt();

        //search a found body for expression statements with callExpressions
        if (block != null) {
            for (Statement statement : block.getStatements()) {

                if (statement instanceof ExpressionStmt expressionStmt) {
                    Expression expression = expressionStmt.getExpression();

                    //store the name of the found expression
                    if (expression instanceof MethodCallExpr call) {
                        if (!methodCallRanges.contains(call.getRange().get())) {
                            callExpr = call;
                            nameOfCall = call.getName();
                            break;
                        }
                    }
                }
            }
        }

        //if a name hast been found, check if it fits the declaration
        if (nameOfCall != null
                && nameOfCall.equals(nameOfDeclaration)) {
            methodCallRanges.add(callExpr.getRange().get());
            createNewTraceNode();

            //set link, out-link and index of out
            int lastAddedIndex = current.getChildrenIndices()
                                .get(current.getChildrenIndices().size() - 1);
            TraceNode lastAdded = nodeList.get(lastAddedIndex);

            //check if ranges are present, should always be true due to preprocessing
            if (nameOfCall.getRange().isPresent()
                    && nameOfDeclaration.getRange().isPresent()) {
                lastAdded.setLink(nameOfCall.getRange().get());
                lastAdded.setOutLink(nameOfDeclaration.getRange().get());
            }
            lastAdded.setOut(lastAdded.getParentIndex());

            return true;
        }

        return false;
    }

    /**
     * advance through all parsable code of the current node and save ranges which are not turned into their own tracenodes in a list,
     * while creating new child-tracenodes for specific codetypes.
     * @param childrenOfCurrent the list of code blocks in the current node
     * @param nextRangeToIgnore range of the next child tracenode, necessary in order to skip it while adding ranges
     */
    private void fillRanges(List<Node> childrenOfCurrent, Range nextRangeToIgnore) {

        boolean skipNext = false;

        for (int i = 0; i < childrenOfCurrent.size();) {

            Node currentNode = childrenOfCurrent.get(i);

            // determine the range of the next child
            if (nextRangeToIgnore == null) {
                if (processChild()) {
                    nextRangeToIgnore = new Range(nodeOfCurrent.getRange().get().end.nextLine(),
                                                  nodeOfCurrent.getRange().get().end.nextLine());
                } else {
                    TraceNode nextChild = nodeList.get(current.getChildrenIndices().get(current.getChildrenIndices().size() - 1));
                    nextRangeToIgnore = (nextChild.getLink() == null) ? traceMap.get(Integer.parseInt(nextChild.getTraceID())).getRange().get() : nextChild.getLink();
                }
            }

            markStatementsInChild(currentNode, nextRangeToIgnore);

            //  current range is a child, let it resolve and wait for the next child
            if (currentNode.getRange().get().contains(nextRangeToIgnore)) {
                nextRangeToIgnore = null;
                skipNext = true;
            }

            // if the next child lies ahead, advance and safe current range in ranges if the skip flag isn't set (i.e. the current range isn't a child)
            else {
                if (skipNext) {
                    skipNext = false;
                } else {
                    if (!current.getRanges().contains(currentNode.getRange().get())) {
                        current.addRange(currentNode.getRange().get());
                    }
                }
                i++;
            }
        }

        // if the current node is a forStmt, and it has iteration steps, add them to the ranges
        if (nodeOfCurrent instanceof ForStmt forStmt) {
            forStmt.getUpdate().forEach(node -> current.addRange(node.getRange().get()));
        }
    }

    /**
     * private method used by {@link #fillRanges} to determine whether the current statement is a child node in which certain
     * codeblocks are always executed (like the condition in an if statement) in order to mark it.
     * @param currentNode Node currently being analyzed
     * @param nextRangeToIgnore next child in case it lies within the current Node
     */
    private void markStatementsInChild(final Node currentNode, final Range nextRangeToIgnore) {
        if (currentNode instanceof IfStmt ifStmt) {
            fillRanges(List.of(ifStmt.getCondition()), nextRangeToIgnore);
        } else if (currentNode instanceof ForStmt forStmt) {
            List<Node> tempRanges = new ArrayList<>(forStmt.getInitialization());
            if (forStmt.getCompare().isPresent()) {
                tempRanges.add(forStmt.getCompare().get());
            }
            fillRanges(tempRanges, nextRangeToIgnore);
        } else if (currentNode instanceof WhileStmt whileStmt) {
            fillRanges(List.of(whileStmt.getCondition()), nextRangeToIgnore);
        } else if (currentNode instanceof ForEachStmt forEachStmt) {
            fillRanges(List.of(forEachStmt.getVariable(), forEachStmt.getIterable()), nextRangeToIgnore);
        } else if (currentNode instanceof DoStmt doStmt) {
            fillRanges(List.of(doStmt.getCondition()), nextRangeToIgnore);
        }
    }

    /**
     * Get the block-statement surrounding a call-expression.
     * Should only be called from {@link #createMethodCallTraceNode()}.
     *
     * @return A {@link BlockStmt} within which the call for the current
     * AST node is located, null if none is found.
     */
    private BlockStmt getBlockStmt() {
        BlockStmt block = null;

        //check if call is within a method
        if (nodeOfCurrent instanceof NodeWithOptionalBlockStmt<?> method) {
            if (method.getBody().isPresent()) {
                block = method.getBody().get();
            }
        }

        //check if call is in a statement, i.e. a then -or else clause
        if (nodeOfCurrent instanceof Statement stmt) {
            if (stmt instanceof BlockStmt b) {
                block = b;
            }
        }

        //check if call is in a loop
        if (nodeOfCurrent instanceof NodeWithBody<?> loop) {
            Statement body = loop.getBody();
            if (body instanceof BlockStmt z) {
                block = z;
            }
        }
        return block;
    }

    /**
     * Gets the nodes created by this preprocessor.
     *
     * @return A List containing the created {@link TraceNode}
     * objects.
     */
    public List<TraceNode> getNodeList() {
        return this.nodeList;
    }

    /**
     * Convert the node list to a String representation.
     *
     * @return A String containing a representation of
     * each node with the value for every field.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (TraceNode node : nodeList) {
            nodeToString(builder, node);
        }
        return builder.toString();
    }

    private void nodeToString(final StringBuilder builder, final TraceNode node) {
        builder.append("\nTraceID: ").append(node.getTraceID())
               .append("\nChildren: ").append(node.getChildrenIndices())
               .append("\nRanges: ").append(node.getRanges())
               .append("\nLink: ").append(node.getLink())
               .append("\nOutlink: ").append(node.getOutLink())
               .append("\nOut: ").append(node.getOutIndex())
               .append("\nParent: ").append(node.getParentIndex())
               .append("\n");
    }
}
