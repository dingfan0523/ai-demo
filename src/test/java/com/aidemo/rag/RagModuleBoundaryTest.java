package com.aidemo.rag;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagModuleBoundaryTest {

    @Test
    void ragModuleDoesNotDependOnMcpInternals() throws IOException {
        Path ragRoot = Path.of("src", "main", "java", "com", "aidemo", "rag");

        try (var paths = Files.walk(ragRoot)) {
            List<Path> offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "com.aidemo.mcp"))
                    .toList();

            assertThat(offenders).isEmpty();
        }
    }

    private boolean contains(Path path, String text) {
        try {
            return Files.readString(path).contains(text);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
