package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

public record CreatePublisherInput(
    String code,
    String name,
    String description,
    String scope,
    Boolean deprecated,
    String settings
) {}
