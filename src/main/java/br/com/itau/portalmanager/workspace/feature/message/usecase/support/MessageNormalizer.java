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
                normalizeServiceIdentifier(input.serviceIdentifier()),
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
                normalizeServiceIdentifier(input.serviceIdentifier()),
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

    public String normalizeServiceIdentifierFilter(String serviceIdentifier) {
        return normalizeServiceIdentifier(serviceIdentifier);
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

    private String normalizeServiceIdentifier(String value) {
        return trimOptional(value);
    }

    private String normalizeLocale(String value) {
        String normalized = trimOptional(value);
        if (normalized == null) {
            return null;
        }

        Locale locale = Locale.forLanguageTag(normalized.replace('_', '-'));
        String languageTag = locale.toLanguageTag();

        return "und".equals(languageTag) ? normalized : languageTag;
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
