package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.FeatureContextValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureContextCommandService {

    private final FeatureContextRepository repository;
    private final FeatureContextValidator validator;

    public FeatureContextCommandService(FeatureContextRepository repository) {
        this(repository, new FeatureContextValidator());
    }

    @Autowired
    public FeatureContextCommandService(FeatureContextRepository repository, FeatureContextValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public FeatureContextOutput create(CreateFeatureContextInput input) {
        validator.validateCreate(input, input != null && repository.existsByCode(input.code()),
                input != null && repository.existsByName(input.name()));
        return FeatureContextOutput.from(repository.save(
                new FeatureContext(input.code(), input.name(), input.description(), now())
        ));
    }

    @Transactional
    public FeatureContextOutput update(String identifier, UpdateFeatureContextInput input) {
        FeatureContext context = required(identifier);
        validator.validateUpdate(input, input != null && !context.getName().equals(input.name())
                && repository.existsByName(input.name()));
        context.update(input.name(), input.description(), now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    public FeatureContextOutput activate(String identifier) {
        FeatureContext context = required(identifier);
        context.activate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    public FeatureContextOutput inactivate(String identifier) {
        FeatureContext context = required(identifier);
        context.inactivate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    public FeatureContextOutput delete(String identifier) {
        FeatureContext context = required(identifier);
        if (!context.getFeatures().isEmpty()) {
            throw new IllegalStateException("Feature context with features cannot be quarantined");
        }
        context.quarantine(now());
        return FeatureContextOutput.from(context);
    }

    private FeatureContext required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature context not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
