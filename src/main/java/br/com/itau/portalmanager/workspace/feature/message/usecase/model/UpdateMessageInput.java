package br.com.itau.portalmanager.workspace.feature.message.usecase.model;

public record UpdateMessageInput(
        Long version,
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation
) {
}
