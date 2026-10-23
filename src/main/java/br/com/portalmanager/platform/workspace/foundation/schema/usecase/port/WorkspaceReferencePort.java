package br.com.portalmanager.platform.workspace.foundation.schema.usecase.port;

public interface WorkspaceReferencePort {

    Long resolveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);
}
