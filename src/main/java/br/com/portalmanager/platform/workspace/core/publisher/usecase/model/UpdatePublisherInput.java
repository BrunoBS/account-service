package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

public record UpdatePublisherInput(
    Long version,
    String name,
    String description,
    String scope,
    Boolean deprecated,
    String settings
) {}
