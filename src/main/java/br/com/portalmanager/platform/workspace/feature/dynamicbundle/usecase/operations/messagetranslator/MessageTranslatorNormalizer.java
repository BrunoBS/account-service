package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import java.util.IllformedLocaleException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class MessageTranslatorNormalizer implements SchemaPayloadNormalizer {

    @Override
    public JsonNode normalizePayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode object)) return payload;
        ObjectNode normalized = object.deepCopy();
        JsonNode locale = normalized.get("locale");
        if (locale != null && locale.isString()) normalized.put("locale", normalizeLocale(locale.asString()));
        for (String field : new String[] {"title", "detail", "suggestion"}) {
            JsonNode value = normalized.get(field);
            if (value != null && value.isString()) normalized.put(field, trim(value.asString()));
        }
        return normalized;
    }

    public CreateMessageTranslatorInput normalize(CreateMessageTranslatorInput input) {
        if (input == null) return null;
        return new CreateMessageTranslatorInput(
                input.version(), normalizeLocale(input.locale()), trim(input.title()),
                trim(input.detail()), trim(input.suggestion()));
    }

    public UpdateMessageTranslatorInput normalize(UpdateMessageTranslatorInput input) {
        if (input == null) return null;
        return new UpdateMessageTranslatorInput(
                input.version(), normalizeLocale(input.locale()), trim(input.title()),
                trim(input.detail()), trim(input.suggestion()));
    }

    public String normalizeLocaleFilter(String locale) {
        return normalizeLocale(locale);
    }

    private String normalizeLocale(String value) {
        String normalized = trimOptional(value);
        if (normalized == null) return null;
        try {
            return new Locale.Builder().setLanguageTag(normalized.replace('_', '-')).build().toLanguageTag();
        } catch (IllformedLocaleException exception) {
            ValidationResult result = new ValidationResult();
            result.addError("locale", MessageMessageKeys.LOCALE_INVALID);
            throw new ValidationException(result);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
