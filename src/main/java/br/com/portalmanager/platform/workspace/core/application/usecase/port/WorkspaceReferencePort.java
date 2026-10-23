package br.com.portalmanager.platform.workspace.core.application.usecase.port;

import java.util.Collection;
import java.util.Map;

public interface WorkspaceReferencePort {
    ResolvedWorkspace resolveActive(String workspaceIdentifier);

    ResolvedWorkspace resolveActiveForUpdate(String workspaceIdentifier);

    Long resolveInternalId(String workspaceIdentifier);

    Map<Long, String> findReferenceIdentifiersForResourceSharing(Collection<Long> ids);

    Map<Long, String> findActiveIdentifiersForResourceSharing(Collection<Long> ids);

    record ResolvedWorkspace(Long id, String identifier, String workspaceType) {}
}
