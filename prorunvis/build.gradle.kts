/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.3/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    id("java")
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("commons-cli:commons-cli:1.4")

    // Dependency for the java parser used in this project
    implementation ("com.github.javaparser:javaparser-symbol-solver-core:3.25.6")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("prorunvis.ProRunVis")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.jar{
    archiveBaseName.set("prorunvis")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE;
    manifest{
        attributes["Main-Class"] = application.mainClass
    }

    from({
        configurations.compileClasspath.get().map { if(it.isDirectory) it else zipTree(it) }
    })
}
