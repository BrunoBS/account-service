package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice;

public record UpdateMicroserviceInput(
        Long version, String code, String name, String description, String settings) {}
