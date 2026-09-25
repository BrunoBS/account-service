package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.repository.FeatureScopeTypeRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureCommandService {
    private final FeatureRepository features;
    private final ServiceRepository services;
    private final FeatureScopeTypeRepository scopes;

    public FeatureCommandService(FeatureRepository features, ServiceRepository services,
                                 FeatureScopeTypeRepository scopes) {
        this.features = features; this.services = services; this.scopes = scopes;
    }

    @Transactional
    public Feature create(String code, String name, String description, String serviceIdentifier, String settings) {
        if (features.existsByCode(code)) throw new IllegalArgumentException("Feature code already exists");
        Service service = requiredService(serviceIdentifier);
        return features.save(new Feature(code, name, description, service, settings, now()));
    }

    @Transactional
    public Feature update(String identifier, String name, String description, String serviceIdentifier, String settings) {
        Feature feature = requiredFeature(identifier);
        Service service = requiredService(serviceIdentifier);
        if (!feature.getService().getIdentifier().equals(service.getIdentifier())) feature.changeService(service, now());
        feature.update(name, description, settings, now());
        return feature;
    }

    @Transactional
    public Feature activate(String identifier) { Feature f = requiredFeature(identifier); f.activate(now()); return f; }

    @Transactional
    public Feature inactivate(String identifier) { Feature f = requiredFeature(identifier); f.inactivate(now()); return f; }

    @Transactional
    public Feature delete(String identifier) { Feature f = requiredFeature(identifier); f.quarantine(now()); return f; }

    @Transactional
    public Feature associateScope(String identifier, String scopeCode) {
        Feature feature = requiredFeature(identifier);
        FeatureScopeType scope = scopes.findById(scopeCode).orElseThrow(() -> new IllegalArgumentException("Feature scope not found"));
        feature.addScope(scope); return feature;
    }

    @Transactional
    public Feature removeScope(String identifier, String scopeCode) {
        Feature feature = requiredFeature(identifier);
        FeatureScopeType scope = scopes.findById(scopeCode).orElseThrow(() -> new IllegalArgumentException("Feature scope not found"));
        feature.removeScope(scope); return feature;
    }

    private Feature requiredFeature(String identifier) {
        return features.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Feature not found"));
    }
    private Service requiredService(String identifier) {
        return services.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }
    private LocalDateTime now() { return LocalDateTime.now(); }
}
