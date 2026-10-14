package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.UpdateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.context.ContextValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextCommandService {
    private final ContextRepository repository;
    private final ContextFinder finder;
    private final ContextValidator validator;

    public ContextCommandService(ContextRepository repository, ContextFinder finder,
        ContextValidator validator) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CONTEXT_CREATED", resourceType = "FEATURE_CONTEXT")
    public ContextOutput create(CreateContextInput input) {
        validator.validateForCreate(input);
        Context context = repository.save(new Context(input.code(), input.name(), input.description(), LocalDateTime.now()));
        repository.flush();
        return ContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_UPDATED", resourceType = "FEATURE_CONTEXT")
    public ContextOutput update(String contextIdentifier, UpdateContextInput input) {
        Context context = finder.findForUpdate(contextIdentifier);
        context.requireActive();
        validator.validateForUpdate(context, input);
        context.update(input.name(), input.description(), LocalDateTime.now());
        repository.flush();
        return ContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_CONTEXT_ACTIVATED", resourceType = "FEATURE_CONTEXT")
    public ContextOutput activate(String contextIdentifier) {
        Context context = finder.findForUpdate(contextIdentifier);
        context.activate(LocalDateTime.now());
        repository.flush();
        return ContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_CONTEXT_DEACTIVATED", resourceType = "FEATURE_CONTEXT")
    public ContextOutput inactivate(String contextIdentifier) {
        Context context = finder.findForUpdate(contextIdentifier);
        context.inactivate(LocalDateTime.now());
        repository.flush();
        return ContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "FEATURE_CONTEXT_PURGED", resourceType = "FEATURE_CONTEXT")
    public ContextOutput purge(String contextIdentifier) {
        Context context = finder.findForUpdate(contextIdentifier);
        validator.validateForPurge(context);
        ContextOutput removedContext = ContextOutput.from(context);
        repository.delete(context);
        repository.flush();
        return removedContext;
    }
}
