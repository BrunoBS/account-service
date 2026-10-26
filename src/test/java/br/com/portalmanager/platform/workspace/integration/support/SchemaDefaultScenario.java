package br.com.portalmanager.platform.workspace.integration.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogBuilder;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogSetupClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** Seeds common catalog and schema prerequisites through their HTTP clients. */
@Component
public final class SchemaDefaultScenario implements TestScenario<SchemaDefaultScenario> {
    private final CatalogSetupClient catalogs;
    private final SchemaScenario schemas;
    private final SchemaFactory schemaFactory = new SchemaFactory();

    public SchemaDefaultScenario(CatalogSetupClient catalogs, SchemaScenario schemas) {
        this.catalogs = Objects.requireNonNull(catalogs);
        this.schemas = Objects.requireNonNull(schemas);
    }

    @Override
    public SchemaDefaultScenario setup() {
        schemas.setup();
        seedCatalogs("/api/v1/parameter-key-type", List.of("CONFIGURATION", "EXPRESSION"));
        seedCatalogs("/api/v1/parameter-key-status-type", List.of("DRAFT", "PUBLISHED"));
        seedCatalogs("/api/v1/share-status-type", List.of("PENDING", "APPROVED", "REJECTED", "REVOKED"));
        seedCatalogs("/api/v1/publication-mode-type", List.of("AUTOMATIC", "MANUAL"));
        seedCatalogs("/api/v1/workspace-type", List.of("ADMIN", "MANAGER"));
        seedCatalogs("/api/v1/application-scope-type", List.of("BACKEND", "FRONTEND", "SHARED"));
        seedSchemas();
        return this;
    }

    private void seedCatalogs(String path, List<String> codes) {
        for (int index = 0; index < codes.size(); index++) {
            String code = codes.get(index);
            if (catalogs.findByCode(path, code).statusCode() == 404) {
                CatalogDTO entry = CatalogBuilder.builder()
                        .withCode(code)
                        .withLabel(code)
                        .withDescription("Integration test catalog entry")
                        .withSortOrder(index + 1)
                        .build();
                catalogs.create(path, entry).expectCreated();
            }
        }
    }

    private void seedSchemas() {
        schemas.bindResource("SERVICE_CONTEXT", "microservice", resource(
                "microservice-resource", "Microservice Resource", "Microservice test schema", "microservice.json"));
        schemas.bindResource("SERVICE_CONTEXT", "feature", resource(
                "feature-resource", "Feature Resource", "Feature test schema", "feature.json"));
        schemas.bindResource("SERVICE_CONTEXT", "feature-group", resource(
                "feature-group-resource", "Feature Group Resource", "Feature group test schema", "feature-group.json"));
        schemas.bindResource("SERVICE_CONTEXT", "feature-context", resource(
                "feature-group-feature-resource", "Feature Group Feature Resource",
                "Feature group association test schema", "feature-group-feature.json"));
        schemas.bindResource("MESSAGE", "message", resource(
                "message-resource", "Message Resource", "Message test schema", "message.json"));
        schemas.bindResource("MESSAGE", "message-translator", resource(
                "message-translator-resource", "Message Translator Resource",
                "Message translator test schema", "message-translator.json"));
        schemas.bindResource("SEGMENTATION", "segment", resource(
                "segment-resource", "Segment Resource", "Segment test schema", "segment.json"));
        schemas.bindResource("SEGMENTATION", "category", resource(
                "category-resource", "Category Resource", "Category test schema", "category.json"));
        schemas.bindResource("PLATFORM", "dynamic-parameter", resource(
                "dynamic-parameter-resource", "Dynamic Parameter Resource",
                "Dynamic parameter test schema", "dynamic-parameter.json"));
        schemas.bindResource("PLATFORM", "dynamic-parameter-publication", resource(
                "dynamic-parameter-publication", "Dynamic Parameter Publication",
                "Dynamic parameter publication test schema", "dynamic-parameter-publication.json"));
        schemas.bindResource("CATALOG", "parameter-key-type", resource(
                "parameter-key-type-settings", "Parameter Key Type Settings",
                "Parameter key type test schema", "parameter-key-type-settings.json"));
        schemas.bindResource("CATALOG", "parameter-key-status-type", resource(
                "parameter-key-status-type-settings", "Parameter Key Status Settings",
                "Parameter key status test schema", "parameter-key-status-type-settings.json"));
    }

    private br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest resource(
            String code, String name, String description, String resourceName) {
        return schemaFactory.resource(code, name, description, "Integration test v1", resourceName);
    }
}
