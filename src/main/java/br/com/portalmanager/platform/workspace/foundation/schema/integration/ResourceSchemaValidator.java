package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared structural validator for Golden resources.
 *
 * <p>The domain supplies a typed {@link ResourceValidationData}; this component turns it into the
 * JSON document validated by the published schema. Null record components are omitted so optional
 * fields mean absence, not an explicit JSON null. The conventional {@code settings} component may
 * stay serialized internally and is expanded back to JSON only at this boundary.</p>
 */
@Component
public class ResourceSchemaValidator {
    private final SchemaResolutionPort resolver;
    private final JsonSchemaValidator json;
    private final ObjectMapper objectMapper;

    public ResourceSchemaValidator(SchemaResolutionPort resolver,
                                   JsonSchemaValidator json,
                                   ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.json = json;
        this.objectMapper = objectMapper;
    }

    public void validate(String resourceType,
                         String resourceCode,
                         ResourceValidationData data,
                         ValidationResult result) {
        JsonNode payload = objectMapper.valueToTree(toSchemaValue(data, null));
        json.validateJson(resolver.resolve(resourceType, resourceCode), payload, "", result);
    }

    private Object toSchemaValue(Object value, String attributeName) {
        if (value == null) return null;

        if ("settings".equals(attributeName) && value instanceof String raw) {
            return json.fromString(raw, attributeName);
        }

        if (value instanceof JsonNode) return value;

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (item != null) normalized.put(String.valueOf(key), toSchemaValue(item, String.valueOf(key)));
            });
            return normalized;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>(collection.size());
            for (Object item : collection) normalized.add(toSchemaValue(item, null));
            return normalized;
        }

        Class<?> type = value.getClass();
        if (!type.isRecord()) return value;

        Map<String, Object> normalized = new LinkedHashMap<>();
        for (RecordComponent component : type.getRecordComponents()) {
            try {
                Object componentValue = component.getAccessor().invoke(value);
                if (componentValue != null) {
                    normalized.put(component.getName(), toSchemaValue(componentValue, component.getName()));
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to read resource validation data", ex);
            }
        }
        return normalized;
    }
}
