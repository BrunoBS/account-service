package br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message;

public record MessageDetails(
        String messageKey, String code, Integer httpStatus, String observation) {}
