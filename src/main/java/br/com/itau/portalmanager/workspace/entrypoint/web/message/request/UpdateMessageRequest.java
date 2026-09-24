package br.com.itau.portalmanager.workspace.entrypoint.web.message.request;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.UpdateMessageInput;

public record UpdateMessageRequest(
        Long version,
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
    public UpdateMessageInput toInput() {
        return new UpdateMessageInput(
                version,
                service,
                messageKey,
                code,
                httpStatus,
                observation
        );
    }
}
