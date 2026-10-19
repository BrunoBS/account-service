package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message;

public record MessageOutput(
        String identifier,
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String lifecycle,
        String observation) {}
