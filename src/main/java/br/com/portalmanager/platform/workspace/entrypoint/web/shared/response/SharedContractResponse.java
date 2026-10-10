package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import java.time.LocalDateTime;

public record SharedContractResponse(
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
    public static SharedContractResponse from(SharedContractOutput output) {
        return new SharedContractResponse(
            output.identifier(),
            output.ownerWorkspaceIdentifier(),
            output.ownerApplicationIdentifier(),
            output.featureIdentifier(),
            output.name(),
            output.description(),
            output.lifecycle(),
            output.createdAt(),
            output.updatedAt()
        );
    }
}
