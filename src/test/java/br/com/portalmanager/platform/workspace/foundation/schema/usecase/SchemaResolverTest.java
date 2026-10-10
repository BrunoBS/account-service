package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.*;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchemaResolverTest {

    private final SchemaConfigurationRepository configurations = mock(SchemaConfigurationRepository.class);
    private final SchemaVersionRepository versions = mock(SchemaVersionRepository.class);
    private final SchemaResolver resolver = new SchemaResolver(
        configurations,
        versions,
        new SchemaOperationValidator()
    );

    @Test
    void missingConfigurationResolvesPublishedDefaultFromSameResourceType() {
        bindDefault("CATALOG");
        assertThat(resolver.resolve("CATALOG", "lifecycle-type")).isEqualTo("{\"type\":\"object\"}");
        verify(configurations).findByResourceTypeAndResourceCode("CATALOG", "DEFAULT");
    }

    @Test
    void publishedDefinitionWinsOverNewerDraft() {
        Schema schema = schema("catalog", SchemaScopeTypeCode.platform());
        SchemaConfiguration binding = new SchemaConfiguration("CATALOG", "lifecycle-type", schema, LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode("CATALOG", "lifecycle-type")).thenReturn(
            Optional.of(binding)
        );
        SchemaVersion published = new SchemaVersion(
            schema,
            2,
            "v2",
            "{\"type\":\"object\"}",
            SchemaVersionStatusTypeCode.published(),
            LocalDateTime.now()
        );
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
                schema.getId(),
                SchemaVersionStatusTypeCode.published()
            )
        ).thenReturn(Optional.of(published));
        assertThat(resolver.resolve("CATALOG", "lifecycle-type")).isEqualTo(published.getDefinition());
    }

    @Test
    void differentCaseDoesNotMatchTheSpecificResourceCode() {
        bindDefault("CATALOG");
        assertThat(resolver.resolve("CATALOG", "LIFECYCLE_TYPE")).isEqualTo("{\"type\":\"object\"}");
        verify(configurations).findByResourceTypeAndResourceCode("CATALOG", "LIFECYCLE_TYPE");
        verify(configurations, never()).findByResourceTypeAndResourceCode("CATALOG", "lifecycle-type");
    }

    @Test
    void lowercaseDefaultIsQueriedLiterallyBeforeTheFallback() {
        bindDefault("CATALOG");
        resolver.resolve("CATALOG", "default");
        verify(configurations).findByResourceTypeAndResourceCode("CATALOG", "default");
        verify(configurations).findByResourceTypeAndResourceCode("CATALOG", "DEFAULT");
    }

    @Test
    void noPublicationFallsBackWithoutRejectingConsumer() {
        bindDefault("PUBLISHER");
        Schema schema = schema("publisher", SchemaScopeTypeCode.platform());
        var binding = new SchemaConfiguration("PUBLISHER", "WEB_SOCKET", schema, LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode("PUBLISHER", "WEB_SOCKET")).thenReturn(
            Optional.of(binding)
        );
        assertThat(resolver.resolve("PUBLISHER", "WEB_SOCKET")).isEqualTo("{\"type\":\"object\"}");
    }

    @Test
    void inactiveConfigurationFallsBack() {
        bindDefault("PUBLISHER");
        Schema schema = schema("publisher", SchemaScopeTypeCode.platform());
        var binding = new SchemaConfiguration("PUBLISHER", "KAAS", schema, LocalDateTime.now());
        binding.inactivate(LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode("PUBLISHER", "KAAS")).thenReturn(Optional.of(binding));
        assertThat(resolver.resolve("PUBLISHER", "KAAS")).isEqualTo("{\"type\":\"object\"}");
        verify(versions, never()).findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
            9L,
            SchemaVersionStatusTypeCode.published()
        );
    }

    @Test
    void missingDefaultReturnsControlledError() {
        assertThatThrownBy(() -> resolver.resolve("PUBLISHER", "WEB_SOCKET")).isInstanceOf(
            br.com.portalmanager.platform.library.messaging.exception.ValidationException.class
        );
        verify(configurations).findByResourceTypeAndResourceCode("PUBLISHER", "DEFAULT");
    }

    @Test
    void missingResourceTypeIsRejectedBeforeLookingUpConfiguration() {
        assertThatThrownBy(() -> resolver.resolve(" ", "WEB_SOCKET")).isInstanceOf(
            br.com.portalmanager.platform.library.messaging.exception.ValidationException.class
        );
        verifyNoInteractions(configurations, versions);
    }

    @Test
    void unPublishedDefaultReturnsControlledError() {
        Schema fallback = mock(Schema.class);
        when(fallback.isActive()).thenReturn(true);
        when(fallback.getId()).thenReturn(11L);
        when(fallback.getIdentifier()).thenReturn("default-schema");
        when(configurations.findByResourceTypeAndResourceCode("FEATURE", "DEFAULT")).thenReturn(
            Optional.of(new SchemaConfiguration("FEATURE", "DEFAULT", fallback, LocalDateTime.now()))
        );
        assertThatThrownBy(() -> resolver.resolve("FEATURE", "unknown")).isInstanceOf(
            br.com.portalmanager.platform.library.messaging.exception.ValidationException.class
        );
    }

    @Test
    void defaultsAreIndependentBetweenResourceTypes() {
        Schema publisherDefault = mock(Schema.class);
        Schema catalogDefault = mock(Schema.class);
        when(publisherDefault.isActive()).thenReturn(true);
        when(catalogDefault.isActive()).thenReturn(true);
        when(publisherDefault.getId()).thenReturn(20L);
        when(catalogDefault.getId()).thenReturn(21L);
        when(configurations.findByResourceTypeAndResourceCode("PUBLISHER", "DEFAULT")).thenReturn(
            Optional.of(new SchemaConfiguration("PUBLISHER", "DEFAULT", publisherDefault, LocalDateTime.now()))
        );
        when(configurations.findByResourceTypeAndResourceCode("CATALOG", "DEFAULT")).thenReturn(
            Optional.of(new SchemaConfiguration("CATALOG", "DEFAULT", catalogDefault, LocalDateTime.now()))
        );
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(20L, SchemaVersionStatusTypeCode.published())
        ).thenReturn(
            Optional.of(
                new SchemaVersion(
                    publisherDefault,
                    1,
                    "v1",
                    "{\"required\":[\"url\"]}",
                    SchemaVersionStatusTypeCode.published(),
                    LocalDateTime.now()
                )
            )
        );
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(21L, SchemaVersionStatusTypeCode.published())
        ).thenReturn(
            Optional.of(
                new SchemaVersion(
                    catalogDefault,
                    1,
                    "v1",
                    "{\"required\":[\"name\"]}",
                    SchemaVersionStatusTypeCode.published(),
                    LocalDateTime.now()
                )
            )
        );
        assertThat(resolver.resolve("PUBLISHER", "MISSING")).isEqualTo("{\"required\":[\"url\"]}");
        assertThat(resolver.resolve("CATALOG", "missing")).isEqualTo("{\"required\":[\"name\"]}");
    }

    @Test
    void eachPublisherTypeUsesItsOwnPublishedJsonSchema() {
        Schema web = schema("web-schema", SchemaScopeTypeCode.platform());
        Schema kaas = mock(Schema.class);
        when(kaas.isActive()).thenReturn(true);
        when(kaas.getId()).thenReturn(10L);
        var webBinding = new SchemaConfiguration("PUBLISHER", "WEB_SOCKET", web, LocalDateTime.now());
        var kaasBinding = new SchemaConfiguration("PUBLISHER", "KAAS", kaas, LocalDateTime.now());
        when(configurations.findByResourceTypeAndResourceCode("PUBLISHER", "WEB_SOCKET")).thenReturn(
            Optional.of(webBinding)
        );
        when(configurations.findByResourceTypeAndResourceCode("PUBLISHER", "KAAS")).thenReturn(
            Optional.of(kaasBinding)
        );
        var webVersion = new SchemaVersion(
            web,
            1,
            "v1",
            "{\"required\":[\"url\"]}",
            SchemaVersionStatusTypeCode.published(),
            LocalDateTime.now()
        );
        var kaasVersion = new SchemaVersion(
            kaas,
            1,
            "v1",
            "{\"required\":[\"bucket\"]}",
            SchemaVersionStatusTypeCode.published(),
            LocalDateTime.now()
        );
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(9L, SchemaVersionStatusTypeCode.published())
        ).thenReturn(Optional.of(webVersion));
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(10L, SchemaVersionStatusTypeCode.published())
        ).thenReturn(Optional.of(kaasVersion));

        assertThat(resolver.resolve("PUBLISHER", "WEB_SOCKET")).isEqualTo(webVersion.getDefinition());
        assertThat(resolver.resolve("PUBLISHER", "KAAS")).isEqualTo(kaasVersion.getDefinition());
    }

    private Schema schema(String code, SchemaScopeTypeCode scope) {
        Schema schema = mock(Schema.class);
        when(schema.getId()).thenReturn(9L);
        when(schema.isActive()).thenReturn(true);
        when(schema.getIdentifier()).thenReturn(code);
        when(schema.getScope()).thenReturn(scope);
        return schema;
    }

    private void bindDefault(String type) {
        Schema fallback = mock(Schema.class);
        when(fallback.isActive()).thenReturn(true);
        when(fallback.getId()).thenReturn(11L);
        when(configurations.findByResourceTypeAndResourceCode(type, "DEFAULT")).thenReturn(
            Optional.of(new SchemaConfiguration(type, "DEFAULT", fallback, LocalDateTime.now()))
        );
        when(
            versions.findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(11L, SchemaVersionStatusTypeCode.published())
        ).thenReturn(
            Optional.of(
                new SchemaVersion(
                    fallback,
                    1,
                    "v1",
                    "{\"type\":\"object\"}",
                    SchemaVersionStatusTypeCode.published(),
                    LocalDateTime.now()
                )
            )
        );
    }
}
