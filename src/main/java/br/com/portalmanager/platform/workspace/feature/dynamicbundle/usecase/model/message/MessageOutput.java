package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;

public record MessageOutput(
        String identifier,
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String lifecycle,
        String observation) {
    public static MessageOutput from(Message message, String microserviceIdentifier) {
        return new MessageOutput(
                message.getIdentifier(),
                message.getVersion(),
                microserviceIdentifier,
                message.getMessageKey(),
                message.getCode(),
                message.getHttpStatus(),
                message.getLifecycle().value(),
                message.getObservation());
    }
}
