package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;

import java.time.LocalDateTime;

public record ContractOutput(
        String identifier,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String featureIdentifier,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ContractOutput from(
            Contract contract,
            String workspaceIdentifier,
            String applicationIdentifier,
            String featureIdentifier,
            String featureName) {
        return new ContractOutput(
                contract.getIdentifier(),
                workspaceIdentifier,
                applicationIdentifier,
                featureIdentifier,
                featureName,
                contract.getDescription(),
                contract.getLifecycle().value(),
                contract.getCreatedAt(),
                contract.getUpdatedAt());
    }
}
