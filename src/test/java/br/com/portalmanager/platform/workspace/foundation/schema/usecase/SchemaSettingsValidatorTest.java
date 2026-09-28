package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.mockito.Mockito.*;

class SchemaSettingsValidatorTest {
    @Test
    void forwardsTheOwningDomainsAttributeNameToJsonValidation() {
        SchemaResolutionPort resolver = mock(SchemaResolutionPort.class);
        JsonSchemaValidator json = mock(JsonSchemaValidator.class);
        JsonNode parsed = mock(JsonNode.class);
        ValidationResult result = new ValidationResult();
        when(resolver.resolve("FEATURE", "flags")).thenReturn("{}");
        when(json.fromString("{\"enabled\":true}", "configuration")).thenReturn(parsed);

        new SchemaSettingsValidator(resolver, json).validate("FEATURE",
                "flags", "configuration", "{\"enabled\":true}", result);

        verify(json).validateJson("{}", parsed, "configuration", result);
    }
}
