package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;

public record MessageTranslationOutput(
        String identifier,
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion,
        String lifecycle
) {
    public static MessageTranslationOutput from(MessageTranslation translation) {
        return new MessageTranslationOutput(
                translation.getIdentifier(),
                translation.getVersion(),
                translation.getLocale(),
                translation.getTitle(),
                translation.getDetail(),
                translation.getSuggestion(),
                translation.getLifecycle().value()
        );
    }
}
