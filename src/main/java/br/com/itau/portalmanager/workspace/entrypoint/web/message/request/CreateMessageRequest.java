package br.com.itau.portalmanager.workspace.entrypoint.web.message.request;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageInput;

public record CreateMessageRequest(
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
    public CreateMessageInput toInput() {
        return new CreateMessageInput(service, messageKey, code, httpStatus, observation);
    }
}
