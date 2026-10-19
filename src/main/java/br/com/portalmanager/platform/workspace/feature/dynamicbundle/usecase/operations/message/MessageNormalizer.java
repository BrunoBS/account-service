package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorNormalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class MessageNormalizer implements SchemaPayloadNormalizer {

    private final MessageTranslatorNormalizer translatorNormalizer;

    public MessageNormalizer(MessageTranslatorNormalizer translatorNormalizer) {
        this.translatorNormalizer = translatorNormalizer;
    }

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        for (String field : List.of("microserviceIdentifier", "observation")) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) normalized.put(field, trimOptional(value.asString()));
        }
        JsonNode messageKey = normalized.get("messageKey");
        if (messageKey != null && messageKey.isString()) normalized.put("messageKey", trim(messageKey.asString()));
        JsonNode code = normalized.get("code");
        if (code != null && code.isString()) normalized.put("code", upper(code.asString()));
        JsonNode translators = normalized.get("translators");
        if (translators instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                array.set(index, translatorNormalizer.normalizePayload(array.get(index)));
            }
        }
        return normalized;
    }

    public CreateMessageInput normalize(CreateMessageInput input) {
        if (input == null) return null;
        return new CreateMessageInput(
                input.version(),
                trimOptional(input.microserviceIdentifier()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation()),
                input.translators() == null
                        ? null
                        : input.translators().stream().map(translatorNormalizer::normalize).toList());
    }

    public UpdateMessageInput normalize(UpdateMessageInput input) {
        if (input == null) return null;
        return new UpdateMessageInput(
                input.version(),
                trimOptional(input.microserviceIdentifier()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation()));
    }

    public String normalizeMicroserviceIdentifierFilter(String value) {
        return trimOptional(value);
    }

    public String normalizeMessageKeyFilter(String value) {
        return trimOptional(value);
    }

    public String normalizeCodeFilter(String value) {
        return upper(value);
    }

    private String upper(String value) {
        String normalized = trimOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
