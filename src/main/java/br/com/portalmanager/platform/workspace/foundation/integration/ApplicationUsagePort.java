package br.com.portalmanager.platform.workspace.foundation.integration;

public interface ApplicationUsagePort {
    boolean isInUse(Long applicationId);
}
