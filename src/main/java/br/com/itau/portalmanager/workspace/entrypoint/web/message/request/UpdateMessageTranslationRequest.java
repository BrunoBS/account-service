package br.com.itau.portalmanager.workspace.entrypoint.web.message.request;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;

public record UpdateMessageTranslationRequest(
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion
) {
    public UpdateMessageTranslationInput toInput() {
        return new UpdateMessageTranslationInput(
                version,
                locale,
                title,
                detail,
                suggestion
        );
    }
}
