package br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractOutput;

import java.time.LocalDateTime;

public record ContractResponse(
        String identifier,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String featureIdentifier,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ContractResponse from(ContractOutput output) {
        return new ContractResponse(
                output.identifier(),
                output.ownerWorkspaceIdentifier(),
                output.ownerApplicationIdentifier(),
                output.featureIdentifier(),
                output.name(),
                output.description(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt());
    }
}
