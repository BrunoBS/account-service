package br.com.portalmanager.platform.workspace.foundation.integration;

public interface FeatureUsagePort {
    boolean isInUse(Long featureId);
}
