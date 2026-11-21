package br.com.portalmanager.platform.workspace.core.workspace.repository.projection;

public interface WorkspaceAuthorizationGroup {
    String getEnvironmentIdentifier();

    String getApplicationIdentifier();

    String getAuthorizerGroup();
}
