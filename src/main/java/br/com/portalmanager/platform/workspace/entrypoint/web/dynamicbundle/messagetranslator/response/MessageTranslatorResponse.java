package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.response;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;

public record MessageTranslatorResponse(
        String identifier,
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion,
        String lifecycle) {
    public static MessageTranslatorResponse from(MessageTranslatorOutput output) {
        return new MessageTranslatorResponse(
                output.identifier(),
                output.version(),
                output.locale(),
                output.title(),
                output.detail(),
                output.suggestion(),
                output.lifecycle());
    }
}
