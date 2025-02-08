package api.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * This class scans all Java files under a source directory (e.g. resources/in)
 * and builds a mapping from (fileName, lineNumber) to the declared variable names.
 */
public class VariableNameMapper {

    // Maps "MyClass.java" -> (lineNumber -> list of declared variable names)
    private final Map<String, Map<Integer, List<String>>> varDecls = new HashMap<>();

    /**
     * Scans all Java files under sourceDir and builds the variable declaration map.
     * @param sourceDir the root path containing the source files (for example, Paths.get("resources/in"))
     * @throws IOException if an I/O error occurs during parsing
     */
    public void buildVarNameMapping(Path sourceDir) throws IOException {
        // Configure JavaParser to use symbol solving
        StaticJavaParser.getConfiguration().setSymbolResolver(
                new JavaSymbolSolver(new CombinedTypeSolver())
        );

        // Create a SourceRoot for the given directory.
        SourceRoot sourceRoot = new SourceRoot(sourceDir);

        // Use the callback version of parse(...) to obtain the localPath directly.
        sourceRoot.parse("", (localPath, absolutePath, parseResult) -> {
            parseResult.getResult().ifPresent(cu -> {
                // Use the localPath (a Path relative to the source root) as the file identifier.
                String relativePath = localPath.toString();
                recordVariableDeclarations(cu, relativePath);
            });
            // Return DONT_SAVE if you do not wish to write back the changes.
            return SourceRoot.Callback.Result.DONT_SAVE;
        });
    }

    /**
     * For each VariableDeclarator in the CompilationUnit, record its declared name and the line number.
     * @param cu the CompilationUnit
     * @param relativePath the relative file path (e.g. "MyClass.java")
     */
    private void recordVariableDeclarations(CompilationUnit cu, String relativePath) {
        // For mapping purposes we only use the simple file name (e.g. "MyClass.java")
        String simpleFile = extractSimpleFileName(relativePath);
        cu.findAll(VariableDeclarator.class).forEach(varDecl -> {
            varDecl.getName().getRange().ifPresent(range -> {
                int line = range.begin.line;
                String varName = varDecl.getNameAsString();
                varDecls
                        .computeIfAbsent(simpleFile, k -> new HashMap<>())
                        .computeIfAbsent(line, k -> new ArrayList<>())
                        .add(varName);
            });
        });
    }

    /**
     * Extracts the simple file name from a relative path.
     * @param pathString the relative path string
     * @return the file name (for example, "MyClass.java")
     */
    private String extractSimpleFileName(String pathString) {
        File f = new File(pathString);
        return f.getName();
    }

    /**
     * Returns the mapping of file names to (line number → list of variable names).
     * @return a Map where keys are file names and values are maps of line numbers to variable names.
     */
    public Map<String, Map<Integer, List<String>>> getVarDeclMap() {
        return varDecls;
    }
}