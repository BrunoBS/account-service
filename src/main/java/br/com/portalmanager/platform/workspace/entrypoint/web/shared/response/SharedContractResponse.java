package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractOutput;
import java.time.LocalDateTime;

public record SharedContractResponse(String identifier, String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier, String name, String description, String lifecycle,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static SharedContractResponse from(SharedContractOutput o) {
        return new SharedContractResponse(o.identifier(), o.ownerWorkspaceIdentifier(), o.ownerApplicationIdentifier(),
                o.name(), o.description(), o.lifecycle(), o.createdAt(), o.updatedAt());
    }
}
