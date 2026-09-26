package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

public record UpdateMessageInput(
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
}
