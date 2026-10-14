package br.com.portalmanager.platform.workspace.core.application.repository.projection;

public interface ApplicationReference {
    Long getId();
    Long getWorkspaceId();
    String getIdentifier();
    String getName();
    String getAlias();
    String getAcronym();
    String getApplicationScope();
    String getAuthorizerGroup();
}
