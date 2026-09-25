package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersion;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersionStatus;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaResolverTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final SchemaResolver resolver = new SchemaResolver(schemaRepository, versionRepository);

    @Test
    void shouldResolveLatestPublishedPlatformSchema() {
        Schema schema = activeSchema("application", "schema-app");
        SchemaVersion version = publishedVersion(schema, "version-app", 3);

        when(schemaRepository.findByTypeScopeAndCode("application", "PLATFORM", null, "application"))
                .thenReturn(Optional.of(schema));
        when(versionRepository.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                null,
                SchemaVersionStatus.PUBLISHED
        )).thenReturn(Optional.of(version));

        SchemaResolution resolution = resolver.resolvePlatform("application");

        assertThat(resolution.requestedSchemaType()).isEqualTo("application");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("application");
        assertThat(resolution.schemaVersion()).isEqualTo(3);
        assertThat(resolution.fallback()).isFalse();
    }

    @Test
    void shouldFallbackToDefaultPlatformSchema() {
        Schema fallback = activeSchema("default", "schema-default");
        SchemaVersion version = publishedVersion(fallback, "version-default", 1);

        when(schemaRepository.findByTypeScopeAndCode("application", "PLATFORM", null, "application"))
                .thenReturn(Optional.empty());
        when(schemaRepository.findByTypeScopeAndCode("default", "PLATFORM", null, "default"))
                .thenReturn(Optional.of(fallback));
        when(versionRepository.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                null,
                SchemaVersionStatus.PUBLISHED
        )).thenReturn(Optional.of(version));

        SchemaResolution resolution = resolver.resolvePlatform("application");

        assertThat(resolution.requestedSchemaType()).isEqualTo("application");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("default");
        assertThat(resolution.fallback()).isTrue();
    }

    private Schema activeSchema(String typeCode, String identifier) {
        Schema schema = mock(Schema.class);
        SchemaType type = mock(SchemaType.class);
        when(type.getCode()).thenReturn(typeCode);
        when(type.isActive()).thenReturn(true);
        when(schema.getSchemaType()).thenReturn(type);
        when(schema.isActive()).thenReturn(true);
        when(schema.getIdentifier()).thenReturn(identifier);
        return schema;
    }

    private SchemaVersion publishedVersion(Schema schema, String identifier, int number) {
        SchemaVersion version = mock(SchemaVersion.class);
        when(version.getSchema()).thenReturn(schema);
        when(version.getIdentifier()).thenReturn(identifier);
        when(version.getSchemaVersion()).thenReturn(number);
        when(version.getDefinition()).thenReturn("{\"type\":\"object\"}");
        return version;
    }
}
