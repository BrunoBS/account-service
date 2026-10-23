package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port;

public interface FeatureUsagePort {
    boolean isInUse(Long featureId);
}
