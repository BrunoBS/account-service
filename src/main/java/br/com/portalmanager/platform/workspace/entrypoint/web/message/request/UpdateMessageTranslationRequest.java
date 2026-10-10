package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;

public record UpdateMessageTranslationRequest(
    Long version,
    String locale,
    String title,
    String detail,
    String suggestion
) {
    public UpdateMessageTranslationInput toInput() {
        return new UpdateMessageTranslationInput(version, locale, title, detail, suggestion);
    }
}
