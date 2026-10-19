package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract;

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
        LocalDateTime updatedAt) {}
