package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice;

/** Internal reference contract; numeric identifiers are never returned by web controllers. */
public record MicroserviceReference(Long id, String identifier, String code) {}
