package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;

public record MessageTranslatorOutput(
        String identifier,
        String messageIdentifier,
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion,
        String lifecycle) {
    public static MessageTranslatorOutput from(MessageTranslator translator) {
        return new MessageTranslatorOutput(
                translator.getIdentifier(),
                translator.getMessage().getIdentifier(),
                translator.getVersion(),
                translator.getLocale(),
                translator.getTitle(),
                translator.getDetail(),
                translator.getSuggestion(),
                translator.getLifecycle().value());
    }
}
