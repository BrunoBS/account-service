package br.com.portalmanager.platform.workspace.unit.foundation.schema.usecase;

import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SchemaSettingsValidationAdapterTest {

    @Test
    void forwardsTheOwningDomainsAttributeNameToJsonValidation() {
        SchemaResolutionPort resolver = mock(SchemaResolutionPort.class);
        JsonSchemaValidator json = mock(JsonSchemaValidator.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parsed = mapper.readTree("{\"enabled\":true}");
        ValidationResult result = new ValidationResult();
        when(resolver.resolve("FEATURE", "flags")).thenReturn("{}");

        new SchemaSettingsValidationAdapter(resolver, json, mapper)
                .validate("FEATURE", "flags", "configuration", "{\"enabled\":true}", result);

        verify(json).validate("{}", parsed, "configuration", result);
    }
}
