package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Shared structural validator for Golden HTTP resource payloads.
 *
 * <p>Validation happens against the JSON body as received at the web boundary. This preserves the
 * distinction between an absent optional field and a field explicitly sent as {@code null}.</p>
 */
@Component
public class ResourceSchemaValidator {
    private final SchemaResolutionPort resolver;
    private final JsonSchemaValidator json;

    public ResourceSchemaValidator(SchemaResolutionPort resolver,
                                   JsonSchemaValidator json) {
        this.resolver = resolver;
        this.json = json;
    }

    public void validate(String resourceType,
                         String resourceCode,
                         JsonNode payload) {
        ValidationResult result = new ValidationResult();
        json.validateJson(resolver.resolve(resourceType, resourceCode), payload, "", result);
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
    }
}
