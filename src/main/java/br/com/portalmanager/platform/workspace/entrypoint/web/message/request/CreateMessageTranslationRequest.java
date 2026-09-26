package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;

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
