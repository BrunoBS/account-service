package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category;

public record CreateCategoryInput(
        Long version, String code, String name, String segmentIdentifier) {}
