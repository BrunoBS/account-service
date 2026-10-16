package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message;

public record UpdateMessageInput(
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation) {}
