import com.moowork.gradle.node.npm.NpmTask


plugins{
    id("com.github.node-gradle.node") version "2.2.0"
}

apply(plugin = "base")

node{
    version = "20.11.0"
    download = true
}

tasks.register<NpmTask>("appNpmInstall"){
    setWorkingDir(file("${project.projectDir}/frontend"))  // Make sure this points to frontend
    setArgs(listOf("install", "react-folder-tree@5.1.1", "--force"))
}

tasks.register<NpmTask>("appNpmBuild") {
    setWorkingDir(file("${project.projectDir}/frontend"))  // Make sure this points to frontend
    setArgs(listOf("run", "build"))
}

tasks.named("appNpmBuild"){
    dependsOn(tasks["appNpmInstall"])
}

tasks.named("build"){
    dependsOn(tasks["appNpmBuild"])
}