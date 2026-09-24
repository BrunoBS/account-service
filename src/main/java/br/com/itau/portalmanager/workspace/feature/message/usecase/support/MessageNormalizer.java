package br.com.itau.portalmanager.workspace.feature.message.usecase.support;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageNormalizer {

    public CreateMessageInput normalize(CreateMessageInput input) {
        if (input == null) {
            return null;
        }
        return new CreateMessageInput(
                normalizeService(input.service()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation()),
                input.translations() == null ? null : input.translations().stream()
                        .map(this::normalize)
                        .toList()
        );
    }

    public UpdateMessageInput normalize(UpdateMessageInput input) {
        if (input == null) {
            return null;
        }
        return new UpdateMessageInput(
                input.version(),
                normalizeService(input.service()),
                trim(input.messageKey()),
                upper(input.code()),
                input.httpStatus(),
                trimOptional(input.observation())
        );
    }

    public CreateMessageTranslationInput normalize(CreateMessageTranslationInput input) {
        if (input == null) {
            return null;
        }
        return new CreateMessageTranslationInput(
                normalizeLocale(input.locale()),
                trim(input.title()),
                trim(input.detail()),
                trim(input.suggestion())
        );
    }

    public UpdateMessageTranslationInput normalize(UpdateMessageTranslationInput input) {
        if (input == null) {
            return null;
        }
        return new UpdateMessageTranslationInput(
                input.version(),
                normalizeLocale(input.locale()),
                trim(input.title()),
                trim(input.detail()),
                trim(input.suggestion())
        );
    }

    public String normalizeServiceFilter(String service) {
        return normalizeService(service);
    }

    private String normalizeService(String value) {
        String normalized = trimOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeLocale(String value) {
        String normalized = trimOptional(value);
        if (normalized == null) {
            return null;
        }
        String[] parts = normalized.replace('-', '_').split("_", -1);
        if (parts.length != 2) {
            return normalized;
        }
        return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
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
