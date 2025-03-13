package api.functionality;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import api.upload.storage.StorageProperties;
import api.upload.storage.StorageService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class StaticMethodExtractorService {

    private final StorageService storageService;

    public StaticMethodExtractorService(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<MethodInfo> extractMethods(String projectId) {
        // Use the project-specific input location
        String sourceLocation = storageService.getProjectInLocation(projectId);
        File sourceDir = new File(sourceLocation);

        if (!sourceDir.exists()) {
            System.out.println("Source directory does not exist for project: " + projectId +
                    " at: " + sourceLocation);
            return new ArrayList<>();
        }

        // Configure JavaParser to use a symbol solver
        StaticJavaParser.getParserConfiguration().setSymbolResolver(
                new JavaSymbolSolver(new CombinedTypeSolver())
        );
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(sourceDir.toPath());
        List<MethodInfo> methods = new ArrayList<>();

        // Iterate over all source roots and parse them
        projectRoot.getSourceRoots().forEach(sourceRoot -> {
            try {
                sourceRoot.tryToParse().forEach(cuResult -> {
                    cuResult.getResult().ifPresent(cu -> {
                        // For each method declaration found in the compilation unit, add its info
                        cu.findAll(MethodDeclaration.class).forEach(md -> {
                            String methodName = md.getNameAsString();
                            String range = md.getRange().map(Object::toString).orElse("No Range");
                            String file = cu.getStorage()
                                    .map(storage -> storage.getPath().toString())
                                    .orElse("Unknown File");
                            methods.add(new MethodInfo(methodName, range, file));
                        });
                    });
                });
            } catch (IOException e) {
                throw new RuntimeException("Error parsing source files for project: " + projectId, e);
            }
        });
        return methods;
    }

    public String toJSON(String projectId) {
        List<MethodInfo> methods = extractMethods(projectId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(methods);
    }

    // Inner class to represent method information
    public static class MethodInfo {
        private String methodName;
        private String range; // Represented as a String for easy JSON serialization
        private String file;

        public MethodInfo(String methodName, String range, String file) {
            this.methodName = methodName;
            this.range = range;
            this.file = file;
        }

        // Getters
        public String getMethodName() {
            return methodName;
        }

        public String getRange() {
            return range;
        }

        public String getFile() {
            return file;
        }
    }
}