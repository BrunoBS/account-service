package br.com.portalmanager.platform.workspace.core.application.repository.projection;

public interface ApplicationScopeReference {
    Long getId();
    Long getWorkspaceId();
    String getApplicationScope();
}
