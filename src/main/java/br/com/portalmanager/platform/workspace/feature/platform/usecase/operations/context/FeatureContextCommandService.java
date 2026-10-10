package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.FeatureContextValidator;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CONTEXT_CREATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput create(CreateFeatureContextInput input) {
        validator.validateCreate(
            input,
            input != null && repository.existsByCode(input.code()),
            input != null && repository.existsByName(input.name())
        );
        return FeatureContextOutput.from(
            repository.save(new FeatureContext(input.code(), input.name(), input.description(), now()))
        );
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_UPDATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput update(String identifier, UpdateFeatureContextInput input) {
        FeatureContext context = required(identifier);
        validator.validateUpdate(
            input,
            input != null && !context.getName().equals(input.name()) && repository.existsByName(input.name())
        );
        context.update(input.name(), input.description(), now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_CONTEXT_ACTIVATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput activate(String identifier) {
        FeatureContext context = required(identifier);
        context.activate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_CONTEXT_DEACTIVATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput inactivate(String identifier) {
        FeatureContext context = required(identifier);
        context.inactivate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "FEATURE_CONTEXT_DELETED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput delete(String identifier) {
        FeatureContext context = required(identifier);
        validator.validateDelete(context);
        context.quarantine(now());
        return FeatureContextOutput.from(context);
    }

    private FeatureContext required(String identifier) {
        return repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
