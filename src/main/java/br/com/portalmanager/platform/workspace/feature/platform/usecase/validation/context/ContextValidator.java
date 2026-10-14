package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.context;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.UpdateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.PlatformValidation;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ContextValidator {
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");
    private final ContextRepository repository;
    private final FeatureContextRelationRepository relations;

    public ContextValidator(ContextRepository repository, FeatureContextRelationRepository relations) {
        this.repository = repository;
        this.relations = relations;
    }

    public void validateForCreate(CreateContextInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() != null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        ValidationResult result = new ValidationResult();
        if (repository.existsByCode(input.code())) result.addError("code", PlatformMessageKeys.CODE_DUPLICATE);
        if (repository.existsByName(input.name())) result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        reject(result);
    }

    public void validateForUpdate(Context context, UpdateContextInput input) {
        PlatformValidation.requireInput(input);
        if (input.version() == null) PlatformValidation.reject("version", PlatformMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(context.getVersion(), input.version())) throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!context.getCode().equals(input.code())) result.addError("code", PlatformMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), context.getId())) result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        reject(result);
    }

    public void validateForPurge(Context context) {
        if (relations.findFirstByContextId(context.getId()).isPresent()) {
            PlatformValidation.reject("context", PlatformMessageKeys.CONTEXT_HAS_FEATURES);
        }
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getOffset() > Integer.MAX_VALUE ||
            pageable.getSort().stream().anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(PlatformMessageKeys.CONTEXT_PAGE_INVALID);
        }
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
