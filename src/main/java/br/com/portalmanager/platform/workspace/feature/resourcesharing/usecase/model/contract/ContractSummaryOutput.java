package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract;

public record ContractSummaryOutput(
        String identifier,
        String name,
        String lifecycle,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String featureIdentifier) {}
