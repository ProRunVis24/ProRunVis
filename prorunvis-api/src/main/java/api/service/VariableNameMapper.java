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

public class VariableNameMapper {

    private final Map<String, Map<Integer, List<String>>> varDecls = new HashMap<>();

    public void buildVarNameMapping(Path sourceDir) throws IOException {
        StaticJavaParser.getConfiguration().setSymbolResolver(
                new JavaSymbolSolver(new CombinedTypeSolver())
        );

        SourceRoot sourceRoot = new SourceRoot(sourceDir);

        sourceRoot.parse("", (localPath, absolutePath, parseResult) -> {
            parseResult.getResult().ifPresent(cu -> {
                String relativePath = localPath.toString();
                recordVariableDeclarations(cu, relativePath);
            });
            return SourceRoot.Callback.Result.DONT_SAVE;
        });
    }

    private void recordVariableDeclarations(CompilationUnit cu, String relativePath) {
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

    private String extractSimpleFileName(String pathString) {
        File f = new File(pathString);
        return f.getName();
    }

    public Map<String, Map<Integer, List<String>>> getVarDeclMap() {
        return varDecls;
    }
}
