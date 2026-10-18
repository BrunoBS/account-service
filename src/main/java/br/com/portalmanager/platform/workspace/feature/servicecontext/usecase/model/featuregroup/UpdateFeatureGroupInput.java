package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup;

public record UpdateFeatureGroupInput(Long version, String code, String name, String description) {}
