package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context;

public record UpdateContextInput(Long version, String code, String name, String description) {}
