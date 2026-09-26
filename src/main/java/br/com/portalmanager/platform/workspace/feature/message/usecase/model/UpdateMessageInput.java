package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

public record UpdateMessageInput(
        Long version,
        String serviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
}
