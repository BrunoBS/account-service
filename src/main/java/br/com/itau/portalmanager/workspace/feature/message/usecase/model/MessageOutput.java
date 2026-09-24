package br.com.itau.portalmanager.workspace.feature.message.usecase.model;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;

public record MessageOutput(
        String identifier,
        Long version,
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String lifecycle,
        String observation
) {
    public static MessageOutput from(Message message) {
        return new MessageOutput(
                message.getIdentifier(),
                message.getVersion(),
                message.getServiceCode(),
                message.getMessageKey(),
                message.getCode(),
                message.getHttpStatus(),
                message.getLifecycle().value(),
                message.getObservation()
        );
    }
}
