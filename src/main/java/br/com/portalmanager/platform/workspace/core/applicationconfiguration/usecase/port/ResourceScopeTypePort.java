package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port;

public interface ResourceScopeTypePort {
    boolean existsActive(String code);
}
