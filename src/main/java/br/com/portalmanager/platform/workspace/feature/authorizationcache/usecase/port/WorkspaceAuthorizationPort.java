package br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.port;

import java.util.List;

public interface WorkspaceAuthorizationPort {
    List<String> findWorkspaceIdentifiers(String cursor);
    List<AuthorizationGroup> findGroups(String workspaceIdentifier);

    record AuthorizationGroup(String environmentIdentifier, String applicationIdentifier,
                              String authorizerGroup) {}
}
