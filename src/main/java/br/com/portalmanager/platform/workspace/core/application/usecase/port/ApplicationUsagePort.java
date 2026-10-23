package br.com.portalmanager.platform.workspace.core.application.usecase.port;

public interface ApplicationUsagePort {
    boolean isInUse(Long applicationId);
}
