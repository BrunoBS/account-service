package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;

public record UpdateMessageRequest(
        Long version,
        String serviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
    public UpdateMessageInput toInput() {
        return new UpdateMessageInput(
                version,
                serviceIdentifier,
                messageKey,
                code,
                httpStatus,
                observation
        );
    }
}
