package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeQueryService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

class SchemaResolverTest {

    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaVersionRepository versionRepository = mock(SchemaVersionRepository.class);
    private final SchemaTypeQueryService schemaTypeQueryService = mock(SchemaTypeQueryService.class);
    private final WorkspaceReferenceResolver workspaceReferenceResolver = mock(WorkspaceReferenceResolver.class);
    private final SchemaResolver resolver =
            new SchemaResolver(schemaRepository, versionRepository, schemaTypeQueryService, workspaceReferenceResolver);

    @Test
    void shouldResolveLatestPublishedPlatformSchema() {
        Schema schema = activeSchema("APPLICATION", "schema-app", SchemaScopeTypeCode.platform());
        SchemaVersion version = publishedVersion(schema, "version-app", 3);
        allowPlatformType("APPLICATION", "APPLICATION");
        allowRequiredType("APPLICATION", SchemaScopeTypeCode.platform(), "APPLICATION");

        when(schemaRepository.findByTypeAndScope("APPLICATION", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        published(version);

        SchemaResolution resolution = resolver.resolvePlatform("APPLICATION");

        assertThat(resolution.requestedSchemaType()).isEqualTo("APPLICATION");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("APPLICATION");
        assertThat(resolution.schemaVersion()).isEqualTo(3);
        assertThat(resolution.fallback()).isFalse();
    }

    @Test
    void shouldFallbackToDefaultWhenSpecificSchemaIsMissing() {
        Schema fallback = activeSchema("DEFAULT", "schema-default", SchemaScopeTypeCode.platform());
        SchemaVersion version = publishedVersion(fallback, "version-default", 1);
        allowPlatformType("APPLICATION", "APPLICATION");
        allowRequiredType("DEFAULT", SchemaScopeTypeCode.platform(), "DEFAULT");

        when(schemaRepository.findByTypeAndScope("APPLICATION", "PLATFORM", null))
                .thenReturn(Optional.empty());
        when(schemaRepository.findByTypeAndScope("DEFAULT", "PLATFORM", null))
                .thenReturn(Optional.of(fallback));
        published(version);

        SchemaResolution resolution = resolver.resolvePlatform("APPLICATION");

        assertThat(resolution.requestedSchemaType()).isEqualTo("APPLICATION");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("DEFAULT");
        assertThat(resolution.fallback()).isTrue();
    }

    @Test
    void shouldFallbackToDefaultWhenSpecificSchemaTypeDoesNotExist() {
        Schema fallback = activeSchema("DEFAULT", "schema-default", SchemaScopeTypeCode.platform());
        SchemaVersion version = publishedVersion(fallback, "version-default", 1);
        when(schemaTypeQueryService.findActiveAllowed("ENVIRONMENT_TYPE", SchemaScopeTypeCode.platform()))
                .thenReturn(Optional.empty());
        allowRequiredType("DEFAULT", SchemaScopeTypeCode.platform(), "DEFAULT");
        when(schemaRepository.findByTypeAndScope("DEFAULT", "PLATFORM", null))
                .thenReturn(Optional.of(fallback));
        published(version);

        SchemaResolution resolution = resolver.resolvePlatform("ENVIRONMENT_TYPE");

        assertThat(resolution.requestedSchemaType()).isEqualTo("ENVIRONMENT_TYPE");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("DEFAULT");
        assertThat(resolution.fallback()).isTrue();
        verify(schemaRepository, never()).findByTypeAndScope("ENVIRONMENT_TYPE", "PLATFORM", null);
    }

    @Test
    void shouldResolveWorkspaceWithoutDefaultFallback() {
        Schema schema = activeSchema("APPLICATION", "schema-workspace", SchemaScopeTypeCode.workspace());
        SchemaVersion version = publishedVersion(schema, "version-workspace", 2);
        allowRequiredType("APPLICATION", SchemaScopeTypeCode.workspace(), "APPLICATION");

        when(workspaceReferenceResolver.resolveInternalId("workspace-identifier"))
                .thenReturn(7L);
        when(schemaRepository.findByTypeScopeAndCode(
                "APPLICATION",
                "WORKSPACE",
                7L,
                "custom-application"
        )).thenReturn(Optional.of(schema));
        published(version);

        SchemaResolution resolution = resolver.resolveWorkspace(
                "workspace-identifier",
                "APPLICATION",
                "custom-application"
        );

        assertThat(resolution.schemaVersion()).isEqualTo(2);
        assertThat(resolution.fallback()).isFalse();
        verify(workspaceReferenceResolver).resolveInternalId("workspace-identifier");
    }

    @Test
    void shouldNormalizeRequestedPlatformTypeBeforeResolution() {
        Schema schema = activeSchema("APPLICATION", "schema-app", SchemaScopeTypeCode.platform());
        SchemaVersion version = publishedVersion(schema, "version-app", 1);
        allowPlatformType("APPLICATION", "APPLICATION");
        allowRequiredType("APPLICATION", SchemaScopeTypeCode.platform(), "APPLICATION");

        when(schemaRepository.findByTypeAndScope("APPLICATION", "PLATFORM", null))
                .thenReturn(Optional.of(schema));
        published(version);

        SchemaResolution resolution = resolver.resolvePlatform("application");

        assertThat(resolution.requestedSchemaType()).isEqualTo("APPLICATION");
        assertThat(resolution.resolvedSchemaType()).isEqualTo("APPLICATION");
    }

    private void allowPlatformType(String requestedCode, String canonicalCode) {
        SchemaType schemaType = schemaType(canonicalCode);
        when(schemaTypeQueryService.findActiveAllowed(requestedCode, SchemaScopeTypeCode.platform()))
                .thenReturn(Optional.of(schemaType));
    }

    private void allowRequiredType(String requestedCode, SchemaScopeTypeCode scope, String canonicalCode) {
        SchemaType schemaType = schemaType(canonicalCode);
        when(schemaTypeQueryService.requireActiveAllowed(requestedCode, scope))
                .thenReturn(schemaType);
    }

    private SchemaType schemaType(String canonicalCode) {
        SchemaType schemaType = mock(SchemaType.class);
        when(schemaType.getCode()).thenReturn(canonicalCode);
        when(schemaType.isActive()).thenReturn(true);
        return schemaType;
    }

    private Schema activeSchema(
            String typeCode,
            String identifier,
            SchemaScopeTypeCode scope
    ) {
        Schema schema = mock(Schema.class);
        when(schema.getSchemaType()).thenReturn(SchemaTypeCode.of(typeCode));
        when(schema.getScope()).thenReturn(scope);
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

    private void published(SchemaVersion version) {
        when(versionRepository.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                nullable(Long.class),
                eq(SchemaVersionStatusTypeCode.published())
        )).thenReturn(Optional.of(version));
    }
}
