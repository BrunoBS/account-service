package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

public interface ResourceScopeTypePort {
    boolean existsActive(String code);
}
