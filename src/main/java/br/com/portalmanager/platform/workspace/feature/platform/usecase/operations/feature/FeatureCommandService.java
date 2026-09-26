package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.ServiceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.FeatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureCommandService {
    private final FeatureRepository features;
    private final ServiceRepository services;
    private final FeatureContextRepository contexts;
    private final FeatureValidator validator;

    public FeatureCommandService(
            FeatureRepository features,
            ServiceRepository services,
            FeatureContextRepository contexts
    ) {
        this(features, services, contexts, new FeatureValidator());
    }

    @Autowired
    public FeatureCommandService(
            FeatureRepository features,
            ServiceRepository services,
            FeatureContextRepository contexts,
            FeatureValidator validator
    ) {
        this.features = features;
        this.services = services;
        this.contexts = contexts;
        this.validator = validator;
    }

    @Transactional
    public FeatureOutput create(CreateFeatureInput input) {
        validator.validateCreate(input, input != null && features.existsByCode(input.code()),
                input != null && features.existsByName(input.name()));
        Service service = requiredService(input.serviceIdentifier());
        validator.validateService(service);
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
        validator.validateUpdate(input, input != null && !feature.getName().equals(input.name())
                && features.existsByName(input.name()));
        Service service = requiredService(input.serviceIdentifier());
        validator.validateService(service);
        if (!feature.getService().getIdentifier().equals(service.getIdentifier())) {
            feature.changeService(service, now());
        }
        feature.update(input.name(), input.description(), input.settings(), now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    public FeatureOutput activate(String identifier) {
        Feature feature = requiredFeature(identifier);
        validator.validateService(feature.getService());
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
        validator.validateContext(context);
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
