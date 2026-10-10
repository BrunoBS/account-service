package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

public record UpdateSchemaInput(Long version, String name, String description) {}
