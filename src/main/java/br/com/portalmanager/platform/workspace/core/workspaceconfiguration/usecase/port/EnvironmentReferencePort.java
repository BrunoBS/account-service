package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

import java.util.Collection;
import java.util.Map;

public interface EnvironmentReferencePort {
    Long resolveActiveForUpdate(Long workspaceId, String identifier);

    String resolveIdentifier(Long id);

    Map<Long, String> resolveIdentifiers(Collection<Long> ids);
}
