package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice;

public record UpdateMicroserviceInput(Long version, String code, String name, String description, String settings) {}
