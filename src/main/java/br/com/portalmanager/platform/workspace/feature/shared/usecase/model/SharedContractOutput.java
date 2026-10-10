package br.com.portalmanager.platform.workspace.feature.shared.usecase.model;

import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedContract;
import java.time.LocalDateTime;

public record SharedContractOutput(
    String identifier,
    String ownerWorkspaceIdentifier,
    String ownerApplicationIdentifier,
    String name,
    String description,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SharedContractOutput from(SharedContract c) {
        return new SharedContractOutput(
            c.getIdentifier(),
            c.getOwnerWorkspaceIdentifier(),
            c.getOwnerApplicationIdentifier(),
            c.getName(),
            c.getDescription(),
            c.getLifecycle().value(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
