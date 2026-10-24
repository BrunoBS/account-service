package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaBindingRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

public final class SchemaFactory {
    private final JsonMapper json = JsonMapper.builder().build();

    public CreateSchemaRequest defaultCatalogSchema() {
        var definition = json.createObjectNode()
                .put("type", "object")
                .put("additionalProperties", true);
        return new CreateSchemaRequest("default", "Default", "Default schema for integration tests",
                "Default v1", definition);
    }

    public CreateSchemaRequest workspaceResourceSchema() {
        try (var input = new ClassPathResource("json/workspace-resource-schema.json").getInputStream()) {
            JsonNode definition = json.readTree(input);
            return new CreateSchemaRequest("workspace-resource", "Workspace Resource",
                    "Workspace request contract for integration tests", "Workspace Resource v1", definition);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Workspace test schema fixture", exception);
        }
    }

    public CreateSchemaRequest applicationResourceSchema() {
        try (var input = new ClassPathResource("json/application-resource-schema.json").getInputStream()) {
            JsonNode definition = json.readTree(input);
            return new CreateSchemaRequest("application-resource", "Application Resource",
                    "Application request contract for integration tests", "Application Resource v1", definition);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Application test schema fixture", exception);
        }
    }

    public CreateSchemaRequest resource(
            String code, String name, String description, String versionName, String resourceName) {
        try (var input = new ClassPathResource("schemas/" + resourceName).getInputStream()) {
            JsonNode definition = json.readTree(input);
            return new CreateSchemaRequest(code, name, description, versionName, definition);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load schema test fixture " + resourceName, exception);
        }
    }

    public CreateSchemaBindingRequest binding(String resourceType, String resourceCode, String schemaIdentifier) {
        return new CreateSchemaBindingRequest(resourceType, resourceCode, schemaIdentifier);
    }
}
