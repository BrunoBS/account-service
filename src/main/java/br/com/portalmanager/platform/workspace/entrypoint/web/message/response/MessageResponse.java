package br.com.portalmanager.platform.workspace.entrypoint.web.message.response;

import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;

public record MessageResponse(
        String identifier,
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String lifecycle,
        String observation
) {
    public static MessageResponse from(MessageOutput output) {
        return new MessageResponse(
                output.identifier(),
                output.version(),
                output.microserviceIdentifier(),
                output.messageKey(),
                output.code(),
                output.httpStatus(),
                output.lifecycle(),
                output.observation()
        );
    }
}
