package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaBindingRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaVersionRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.UpdateSchemaBindingRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.UpdateSchemaRequest;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

public final class SchemaFactory extends AbstractTestDataFactory<CreateSchemaRequest, SchemaBuilder> {
    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    protected SchemaBuilder builder() {
        return SchemaBuilder.builder();
    }

    public CreateSchemaRequest valid() {
        return builder().build();
    }

    public CreateSchemaRequest withoutCode() {
        return builder().withCode(null).build();
    }

    public CreateSchemaRequest withInvalidCode() {
        return builder().withCode("Invalid Code").build();
    }

    public CreateSchemaRequest withoutName() {
        return builder().withName(null).build();
    }

    public CreateSchemaRequest withDuplicateCode(String code) {
        return builder().withCode(code).build();
    }

    public CreateSchemaRequest withInvalidDefinition() {
        return builder().withDefinition(json.createArrayNode()).build();
    }

    public CreateSchemaRequest withVersionName(String versionName) {
        return builder().withVersionName(versionName).build();
    }

    public UpdateSchemaRequest update(Long version) {
        return new UpdateSchemaRequest(version, "Updated schema name", "Updated schema description");
    }

    public UpdateSchemaRequest updateWithVersion(Long version, String name) {
        return new UpdateSchemaRequest(version, name, "Updated schema description");
    }

    public UpdateSchemaRequest withStaleVersion(Long version) {
        return new UpdateSchemaRequest(version, "Stale update", "Stale version");
    }

    public CreateSchemaVersionRequest validVersion(String name) {
        return SchemaVersionBuilder.builder().withVersionName(name).build();
    }

    public CreateSchemaVersionRequest withoutVersionDefinition() {
        return SchemaVersionBuilder.builder().withDefinition(null).build();
    }

    public UpdateSchemaBindingRequest updateBinding(Long version, String schemaIdentifier) {
        return new UpdateSchemaBindingRequest(version, schemaIdentifier);
    }

    public UpdateSchemaBindingRequest staleBinding(Long version, String schemaIdentifier) {
        return new UpdateSchemaBindingRequest(version, schemaIdentifier);
    }

    public CreateSchemaBindingRequest validBinding(String resourceType, String resourceCode, String schemaIdentifier) {
        return SchemaBindingBuilder.builder().withResourceType(resourceType).withResourceCode(resourceCode)
                .withSchemaIdentifier(schemaIdentifier).build();
    }

    public CreateSchemaBindingRequest bindingWithoutResourceType(String schemaIdentifier) {
        return SchemaBindingBuilder.builder().withResourceType(null).withSchemaIdentifier(schemaIdentifier).build();
    }

    public CreateSchemaBindingRequest bindingWithoutResourceCode(String schemaIdentifier) {
        return SchemaBindingBuilder.builder().withResourceCode(null).withSchemaIdentifier(schemaIdentifier).build();
    }

    public CreateSchemaBindingRequest bindingWithoutSchema(String resourceType, String resourceCode) {
        return SchemaBindingBuilder.builder().withResourceType(resourceType).withResourceCode(resourceCode).build();
    }

    public CreateSchemaBindingRequest bindingWithUnknownSchema(String resourceType, String resourceCode) {
        return SchemaBindingBuilder.builder().withResourceType(resourceType).withResourceCode(resourceCode)
                .withSchemaIdentifier("missing-schema").build();
    }

    public CreateSchemaRequest defaultCatalogSchema() {
        return builder().withCode("default").withName("Default")
                .withDescription("Default schema for integration tests")
                .withVersionName("Default v1")
                .withDefinition(json.createObjectNode().put("type", "object").put("additionalProperties", true))
                .build();
    }

    public CreateSchemaRequest workspaceResourceSchema() {
        try (var input = new ClassPathResource("json/workspace-resource-schema.json").getInputStream()) {
            JsonNode definition = json.readTree(input);
            return builder().withCode("workspace-resource").withName("Workspace Resource")
                    .withDescription("Workspace request contract for integration tests")
                    .withVersionName("Workspace Resource v1").withDefinition(definition).build();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Workspace test schema fixture", exception);
        }
    }

    public CreateSchemaRequest applicationResourceSchema() {
        try (var input = new ClassPathResource("json/application-resource-schema.json").getInputStream()) {
            JsonNode definition = json.readTree(input);
            return builder().withCode("application-resource").withName("Application Resource")
                    .withDescription("Application request contract for integration tests")
                    .withVersionName("Application Resource v1").withDefinition(definition).build();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Application test schema fixture", exception);
        }
    }

    public CreateSchemaRequest resource(
            String code, String name, String description, String versionName, String resourceName) {
        try (var input = new ClassPathResource("schemas/" + resourceName).getInputStream()) {
            JsonNode definition = json.readTree(input);
            return builder().withCode(code).withName(name).withDescription(description)
                    .withVersionName(versionName).withDefinition(definition).build();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load schema test fixture " + resourceName, exception);
        }
    }

    public CreateSchemaBindingRequest binding(String resourceType, String resourceCode, String schemaIdentifier) {
        return validBinding(resourceType, resourceCode, schemaIdentifier);
    }
}
