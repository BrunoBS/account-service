package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.domain.FeatureContext;
import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureCommandService {
    private final FeatureRepository features;
    private final ServiceRepository services;
    private final FeatureContextRepository contexts;

    public FeatureCommandService(
            FeatureRepository features,
            ServiceRepository services,
            FeatureContextRepository contexts
    ) {
        this.features = features;
        this.services = services;
        this.contexts = contexts;
    }

    @Transactional
    public FeatureOutput create(CreateFeatureInput input) {
        if (features.existsByCode(input.code())) throw new IllegalArgumentException("Feature code already exists");
        if (features.existsByName(input.name())) throw new IllegalArgumentException("Feature name already exists");
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
        if (!feature.getName().equals(input.name()) && features.existsByName(input.name())) {
            throw new IllegalArgumentException("Feature name already exists");
        }
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
    public FeatureOutput associateContext(String identifier, String contextIdentifier) {
        Feature feature = requiredFeature(identifier);
        FeatureContext context = contexts.findByIdentifier(contextIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature context not found"));
        feature.addContext(context);
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput removeContext(String identifier, String contextIdentifier) {
        Feature feature = requiredFeature(identifier);
        FeatureContext context = contexts.findByIdentifier(contextIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature context not found"));
        feature.removeContext(context);
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
