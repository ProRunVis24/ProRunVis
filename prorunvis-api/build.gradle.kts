plugins {
    id ("org.springframework.boot") version "3.0.0"
    id ("io.spring.dependency-management") version "1.1.0"
    id ("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
}

dependencies {
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation ("org.springframework.boot:spring-boot-starter-data-jpa:3.0.0")

    //spring boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Removed conflicting platform dependency

    //commons
    implementation("commons-io:commons-io:2.15.1")

    implementation(project(":prorunvis"))
    //frontend submodule
    implementation(project(":frontend"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyWebApp"){
    from("../frontend/build")
    into("build/resources/main/static")
}

tasks.named("copyWebApp"){
    dependsOn(project(":frontend").tasks["appNpmBuild"])
}

tasks.named("compileJava"){
    dependsOn(tasks["copyWebApp"])
}