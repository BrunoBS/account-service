package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.repository.FeatureScopeTypeRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureCommandService {
    private final FeatureRepository features;
    private final ServiceRepository services;
    private final FeatureScopeTypeRepository scopes;

    public FeatureCommandService(
            FeatureRepository features,
            ServiceRepository services,
            FeatureScopeTypeRepository scopes
    ) {
        this.features = features;
        this.services = services;
        this.scopes = scopes;
    }

    @Transactional
    public FeatureOutput create(CreateFeatureInput input) {
        if (features.existsByCode(input.code())) throw new IllegalArgumentException("Feature code already exists");
        Service service = requiredService(input.serviceIdentifier());
        return FeatureOutput.from(features.save(new Feature(
                input.code(),
                input.name(),
                input.description(),
                service,
                input.settings(),
                now()
        )));
    }

    @Transactional
    public FeatureOutput update(String identifier, UpdateFeatureInput input) {
        Feature feature = requiredFeature(identifier);
        Service service = requiredService(input.serviceIdentifier());
        if (!feature.getService().getIdentifier().equals(service.getIdentifier())) {
            feature.changeService(service, now());
        }
        feature.update(input.name(), input.description(), input.settings(), now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput activate(String identifier) {
        Feature feature = requiredFeature(identifier);
        feature.activate(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput inactivate(String identifier) {
        Feature feature = requiredFeature(identifier);
        feature.inactivate(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput delete(String identifier) {
        Feature feature = requiredFeature(identifier);
        feature.quarantine(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput associateScope(String identifier, String scopeCode) {
        Feature feature = requiredFeature(identifier);
        FeatureScopeType scope = scopes.findById(scopeCode)
                .orElseThrow(() -> new IllegalArgumentException("Feature scope not found"));
        feature.addScope(scope);
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput removeScope(String identifier, String scopeCode) {
        Feature feature = requiredFeature(identifier);
        FeatureScopeType scope = scopes.findById(scopeCode)
                .orElseThrow(() -> new IllegalArgumentException("Feature scope not found"));
        feature.removeScope(scope);
        return FeatureOutput.from(feature);
    }

    private Feature requiredFeature(String identifier) {
        return features.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found"));
    }

    private Service requiredService(String identifier) {
        return services.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
