package br.com.itau.portalmanager.workspace.entrypoint.web.message.request;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageTranslationInput;

public record CreateMessageTranslationRequest(
        String locale,
        String title,
        String detail,
        String suggestion
) {
    public CreateMessageTranslationInput toInput() {
        return new CreateMessageTranslationInput(locale, title, detail, suggestion);
    }
}
