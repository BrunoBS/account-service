package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;

@Component
public class MessageNormalizer implements SchemaPayloadNormalizer {

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        for (String field :
                List.of("microserviceIdentifier", "messageKey", "title", "detail", "suggestion")) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) normalized.put(field, trim(value.asString()));
        }
        JsonNode code = normalized.get("code");
        if (code != null && code.isString()) normalized.put("code", upper(code.asString()));
        JsonNode observation = normalized.get("observation");
        if (observation != null && observation.isString()) {
            normalized.put("observation", trimOptional(observation.asString()));
        }
        JsonNode locale = normalized.get("locale");
        if (locale != null && locale.isString())
            normalized.put("locale", normalizeLocale(locale.asString()));
        JsonNode translators = normalized.get("translators");
        if (translators instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                array.set(index, normalizePayload(array.get(index)));
            }
        }
        return normalized;
    }

    public CreateMessageInput normalize(CreateMessageInput input) {
        if (input == null) {
            return null;
        }
        return new CreateMessageInput(
                input.version(),
                normalizeMicroserviceIdentifier(input.microserviceIdentifier()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation()),
                input.translators() == null
                        ? null
                        : input.translators().stream().map(this::normalize).toList());
    }

    public UpdateMessageInput normalize(UpdateMessageInput input) {
        if (input == null) {
            return null;
        }
        return new UpdateMessageInput(
                input.version(),
                normalizeMicroserviceIdentifier(input.microserviceIdentifier()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation()));
    }

    public CreateMessageTranslatorInput normalize(CreateMessageTranslatorInput input) {
        if (input == null) {
            return null;
        }
        return new CreateMessageTranslatorInput(
                input.version(),
                normalizeLocale(input.locale()),
                trim(input.title()),
                trim(input.detail()),
                trim(input.suggestion()));
    }

    public UpdateMessageTranslatorInput normalize(UpdateMessageTranslatorInput input) {
        if (input == null) {
            return null;
        }
        return new UpdateMessageTranslatorInput(
                input.version(),
                normalizeLocale(input.locale()),
                trim(input.title()),
                trim(input.detail()),
                trim(input.suggestion()));
    }

    public String normalizeMicroserviceIdentifierFilter(String microserviceIdentifier) {
        return normalizeMicroserviceIdentifier(microserviceIdentifier);
    }

    public String normalizeMessageKeyFilter(String messageKey) {
        return trimOptional(messageKey);
    }

    public String normalizeCodeFilter(String code) {
        return upper(code);
    }

    public String normalizeLocaleFilter(String locale) {
        return normalizeLocale(locale);
    }

    private String normalizeMicroserviceIdentifier(String value) {
        return trimOptional(value);
    }

    private String normalizeLocale(String value) {
        String normalized = trimOptional(value);
        if (normalized == null) {
            return null;
        }

        try {
            return new Locale.Builder()
                    .setLanguageTag(normalized.replace('_', '-'))
                    .build()
                    .toLanguageTag();
        } catch (IllformedLocaleException exception) {
            ValidationResult result = new ValidationResult();
            result.addError("locale", MessageMessageKeys.LOCALE_INVALID);
            throw new ValidationException(result);
        }
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
