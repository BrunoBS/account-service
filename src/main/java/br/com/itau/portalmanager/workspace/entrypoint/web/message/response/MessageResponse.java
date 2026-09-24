package br.com.itau.portalmanager.workspace.entrypoint.web.message.response;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.MessageOutput;

public record MessageResponse(
        String identifier,
        Long version,
        String service,
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
                output.service(),
                output.messageKey(),
                output.code(),
                output.httpStatus(),
                output.lifecycle(),
                output.observation()
        );
    }
}
