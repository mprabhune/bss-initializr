package com.discover.bss_initializr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProjectGenerator {

    public static byte[] generate(ProjectRequest projectRequest) throws IOException {
        String artifactId = projectRequest.getArtifactId();
        Path tempDir = Files.createTempDirectory(artifactId);

        // Create the base directory structure
        Path javaBaseDir = tempDir.resolve("src/main/java/com/example");
        Path testBaseDir = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(javaBaseDir);
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.createDirectories(tempDir.resolve("src/main/resources/static"));
        Files.createDirectories(testBaseDir);

        // Combine hardcoded and user-specified dependencies
        List<String> hardcodedDependencies = List.of(
                "org.springframework.boot:spring-boot-starter-web",
                "org.springframework.boot:spring-boot-starter-thymeleaf",
                "org.springframework.cloud:spring-cloud-starter:4.2.0"
        );
        List<String> allDependencies = new ArrayList<>(hardcodedDependencies);
        if (projectRequest.getDependencies() != null) {
            allDependencies.addAll(projectRequest.getDependencies());
        }

        // Generate files
        Files.writeString(tempDir.resolve("build.gradle"), generateBuildGradle(allDependencies));
        Files.writeString(tempDir.resolve("settings.gradle"), "rootProject.name = '" + artifactId + "'");

        // Add gradle wrapper files
        addGradleWrapper(tempDir);

        // Add a sample Java class file
        String javaClassContent = """
            package com.example;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class CustomProjectApplication {

                public static void main(String[] args) {
                    SpringApplication.run(CustomProjectApplication.class, args);
                }
            }
        """;
        Files.writeString(javaBaseDir.resolve("CustomProjectApplication.java"), javaClassContent);

        // Add a sample test file
        String testClassContent = """
            package com.example;

            import org.junit.jupiter.api.Test;
            import org.springframework.boot.test.context.SpringBootTest;

            @SpringBootTest
            class CustomProjectApplicationTests {

                @Test
                void contextLoads() {
                }
            }
        """;
        Files.writeString(testBaseDir.resolve("CustomProjectApplicationTests.java"), testClassContent);

        // Add a sample application.properties file
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "server.port=8080");

        // Add a .gitignore file
        Files.writeString(tempDir.resolve(".gitignore"), """
            # Ignore Gradle files
            .gradle/
            build/
            out/

            # Ignore IntelliJ IDEA files
            .idea/
            *.iml

            # Ignore other system files
            .DS_Store
            """);

        // Zip the directory
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            Files.walk(tempDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                try {
                    ZipEntry zipEntry = new ZipEntry(tempDir.relativize(path).toString());
                    zipOutputStream.putNextEntry(zipEntry);
                    Files.copy(path, zipOutputStream);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Cleanup
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

        return byteArrayOutputStream.toByteArray();
    }

    private static String generateBuildGradle(List<String> dependencies) {
        StringBuilder buildGradle = new StringBuilder();
        buildGradle.append("""
            plugins {
                id 'org.springframework.boot' version '3.0.0'
                id 'io.spring.dependency-management' version '1.1.0'
                id 'java'
            }
            group = 'com.example'
            version = '0.0.1-SNAPSHOT'
            sourceCompatibility = '17'

            repositories {
                mavenCentral()
            }

            dependencies {
        """);

        // Append all dependencies to the build.gradle file
        for (String dependency : dependencies) {
            buildGradle.append("    implementation '").append(dependency).append("'\n");
        }

        buildGradle.append("""
            }

            test {
                useJUnitPlatform()
            }
        """);

        return buildGradle.toString();
    }

    private static void addGradleWrapper(Path tempDir) throws IOException {
        // Add Gradle wrapper scripts and properties
        Path wrapperDir = tempDir.resolve("gradle/wrapper");
        Files.createDirectories(wrapperDir);

        Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            distributionUrl=https\\://services.gradle.org/distributions/gradle-8.0-bin.zip
        """);

        Files.writeString(tempDir.resolve("gradlew"), """
            #!/usr/bin/env sh
            BASE_DIR=$(dirname "$0")
            exec "$BASE_DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
        """);

        Files.writeString(tempDir.resolve("gradlew.bat"), """
            @echo off
            set BASE_DIR=%~dp0
            java -jar "%BASE_DIR%gradle\\wrapper\\gradle-wrapper.jar" %*
        """);
    }
}
