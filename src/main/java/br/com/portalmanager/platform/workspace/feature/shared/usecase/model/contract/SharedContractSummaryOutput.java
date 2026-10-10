package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract;

public record SharedContractSummaryOutput(
    String identifier,
    String name,
    String lifecycle,
    String ownerWorkspaceIdentifier,
    String ownerApplicationIdentifier,
    String featureIdentifier
) {}
