package br.com.portalmanager.platform.workspace.feature.message.domain;

public record MessageDetails(String messageKey, String code, Integer httpStatus, String observation) {}
