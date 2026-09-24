package br.com.itau.portalmanager.workspace.entrypoint.web.message.response;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageTranslationOutput;

public record MessageTranslationResponse(
        String identifier,
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion,
        String lifecycle
) {
    public static MessageTranslationResponse from(MessageTranslationOutput output) {
        return new MessageTranslationResponse(
                output.identifier(),
                output.version(),
                output.locale(),
                output.title(),
                output.detail(),
                output.suggestion(),
                output.lifecycle()
        );
    }
}
