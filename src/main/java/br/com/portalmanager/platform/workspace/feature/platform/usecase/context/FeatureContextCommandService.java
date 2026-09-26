package br.com.portalmanager.platform.workspace.feature.platform.usecase.context;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class FeatureContextCommandService {

    private final FeatureContextRepository repository;

    public FeatureContextCommandService(FeatureContextRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FeatureContextOutput create(CreateFeatureContextInput input) {
        if (repository.existsByCode(input.code())) throw new IllegalArgumentException("Feature context code already exists");
        if (repository.existsByName(input.name())) throw new IllegalArgumentException("Feature context name already exists");
        return FeatureContextOutput.from(repository.save(
                new FeatureContext(input.code(), input.name(), input.description(), now())
        ));
    }

    @Transactional
    public FeatureContextOutput update(String identifier, UpdateFeatureContextInput input) {
        FeatureContext context = required(identifier);
        if (!context.getName().equals(input.name()) && repository.existsByName(input.name())) {
            throw new IllegalArgumentException("Feature context name already exists");
        }
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
