package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract;

import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import java.time.LocalDateTime;

public record SharedContractOutput(
    String identifier,
    String ownerWorkspaceIdentifier,
    String ownerApplicationIdentifier,
    String featureIdentifier,
    String name,
    String description,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SharedContractOutput from(
        SharedContract contract,
        String workspaceIdentifier,
        String applicationIdentifier,
        String featureIdentifier,
        String featureName
    ) {
        return new SharedContractOutput(
            contract.getIdentifier(),
            workspaceIdentifier,
            applicationIdentifier,
            featureIdentifier,
            featureName,
            contract.getDescription(),
            contract.getLifecycle().value(),
            contract.getCreatedAt(),
            contract.getUpdatedAt()
        );
    }
}
