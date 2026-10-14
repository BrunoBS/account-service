package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.junit.jupiter.api.Test;

class PublishedMigrationChecksumTest {

    @Test
    void publishedMigrationsKeepTheChecksumsOfTheAppliedHistory() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream history = classLoader.getResourceAsStream("migration/applied-checksums.csv");
        assertThat(history).as("Applied migration checksum fixture").isNotNull();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(history, StandardCharsets.UTF_8))) {
            List<String> entries = reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
            assertThat(entries).hasSize(44);
            for (String entry : entries) {
                String[] fields = entry.split(",", 2);
                String script = fields[0];
                int expectedChecksum = Integer.parseInt(fields[1]);
                ClassPathResource migration = new ClassPathResource(null, "db/migration/" + script,
                    classLoader, StandardCharsets.UTF_8);
                assertThat(migration.exists()).as("Published migration %s", script).isTrue();
                assertThat(ChecksumCalculator.calculate(migration))
                    .as("Do not reformat or edit applied migration %s; add a new migration", script)
                    .isEqualTo(expectedChecksum);
            }
        }
    }
}
