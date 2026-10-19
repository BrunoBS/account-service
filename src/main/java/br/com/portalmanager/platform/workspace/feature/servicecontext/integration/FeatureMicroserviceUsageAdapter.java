package br.com.portalmanager.platform.workspace.feature.servicecontext.integration;

import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceUsagePort;

import org.springframework.stereotype.Component;

@Component
public class FeatureMicroserviceUsageAdapter implements MicroserviceUsagePort {
    private final FeatureRepository features;

    public FeatureMicroserviceUsageAdapter(FeatureRepository features) {
        this.features = features;
    }

    @Override
    public boolean isInUse(Long microserviceId) {
        return features.existsByMicroservice_Id(microserviceId);
    }
}
