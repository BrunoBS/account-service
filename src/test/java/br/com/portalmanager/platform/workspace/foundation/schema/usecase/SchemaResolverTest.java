package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.*;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaResolverTest {
    private final SchemaConfigurationRepository configurations = mock(SchemaConfigurationRepository.class);
    private final SchemaRepository schemas = mock(SchemaRepository.class);
    private final SchemaVersionRepository versions = mock(SchemaVersionRepository.class);
    private final WorkspaceReferenceResolver workspaces = mock(WorkspaceReferenceResolver.class);
    private final SchemaResolver resolver = new SchemaResolver(configurations, schemas, versions, workspaces);

    @Test
    void missingConfigurationReturnsJsonSchemaConstantWithoutDatabaseDefaultLookup() {
        assertThat(resolver.resolve(SchemaResourceType.CATALOG, "lifecycle-type"))
                .isEqualTo(SchemaDefaults.DEFAULT_JSON_SCHEMA);
        verifyNoInteractions(schemas, versions);
    }

    @Test
    void publishedDefinitionWinsOverNewerDraft() {
        Schema schema = schema("catalog", SchemaScopeTypeCode.platform());
        SchemaConfiguration binding = new SchemaConfiguration(SchemaResourceType.CATALOG,
                "lifecycle-type", schema, LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode(SchemaResourceType.CATALOG, "lifecycle-type"))
                .thenReturn(Optional.of(binding));
        SchemaVersion published = new SchemaVersion(schema, 2, "v2", "{\"type\":\"object\"}",
                SchemaVersionStatusTypeCode.published(), LocalDateTime.now());
        when(versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                schema.getId(), SchemaVersionStatusTypeCode.published())).thenReturn(Optional.of(published));
        assertThat(resolver.resolve(SchemaResourceType.CATALOG, "LIFECYCLE_TYPE"))
                .isEqualTo(published.getDefinition());
    }

    @Test
    void noPublicationFallsBackWithoutRejectingConsumer() {
        Schema schema = schema("publisher", SchemaScopeTypeCode.platform());
        var binding = new SchemaConfiguration(SchemaResourceType.PUBLISHER, "WEB_SOCKET", schema, LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode(SchemaResourceType.PUBLISHER, "WEB_SOCKET"))
                .thenReturn(Optional.of(binding));
        assertThat(resolver.resolve(SchemaResourceType.PUBLISHER, "WEB_SOCKET"))
                .isEqualTo(SchemaDefaults.DEFAULT_JSON_SCHEMA);
    }

    @Test
    void inactiveConfigurationFallsBack() {
        Schema schema = schema("publisher", SchemaScopeTypeCode.platform());
        var binding = new SchemaConfiguration(SchemaResourceType.PUBLISHER, "KAAS", schema, LocalDateTime.now());
        binding.inactivate(LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode(SchemaResourceType.PUBLISHER, "KAAS"))
                .thenReturn(Optional.of(binding));
        assertThat(resolver.resolve(SchemaResourceType.PUBLISHER, "KAAS"))
                .isEqualTo(SchemaDefaults.DEFAULT_JSON_SCHEMA);
        verifyNoInteractions(versions);
    }

    @Test
    void workspaceStillRequiresItsOwnPublishedSchemaWithoutDefaultFallback() {
        when(workspaces.resolveInternalId("workspace-id")).thenReturn(7L);
        assertThatThrownBy(() -> resolver.resolveWorkspace("workspace-id", "settings"))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class);
        verify(schemas).findByScopeAndCode("WORKSPACE", 7L, "settings");
    }

    private Schema schema(String code, SchemaScopeTypeCode scope) {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(9L);
        when(schema.isActive()).thenReturn(true);
        when(schema.getIdentifier()).thenReturn(code);
        when(schema.getScope()).thenReturn(scope);
        return schema;
    }
}
