package br.com.portalmanager.platform.workspace.core.publisher.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherMessageKeys;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PublisherValidator {

    private final ResourceScopeTypeService scopeService;
    private final PublisherRepository repository;

    public PublisherValidator(ResourceScopeTypeService scopeService, PublisherRepository repository) {
        this.scopeService = scopeService;
        this.repository = repository;
    }

    public void validateForCreate(CreatePublisherInput input) {
        if (input == null || input.code() == null) throw new ValidationException(PublisherMessageKeys.CODE_INVALID);
        ValidationResult result = new ValidationResult();
        if (input.version() != null) {
            result.addError("version", PublisherMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
            reject(result);
        }
        if (repository.existsByCode(input.code())) result.addError("code", PublisherMessageKeys.CODE_DUPLICATE);
        validateScope(input.scope(), result);
        reject(result);
    }

    public void validateForUpdate(Publisher publisher, UpdatePublisherInput input) {
        if (input == null || input.version() == null) throw new ValidationException(
            PublisherMessageKeys.VERSION_REQUIRED
        );
        if (!Objects.equals(publisher.getVersion(), input.version())) throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!publisher.getCode().equals(input.code())) result.addError("code", PublisherMessageKeys.CODE_IMMUTABLE);
        validateScope(input.scope(), result);
        reject(result);
    }

    public void validateQuery(String scope, Pageable pageable) {
        Set<String> sortableProperties = Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");
        if (
            pageable.isUnpaged() ||
            pageable.getOffset() > Integer.MAX_VALUE ||
            pageable
                .getSort()
                .stream()
                .anyMatch(order -> !sortableProperties.contains(order.getProperty()))
        ) {
            throw new ValidationException(PublisherMessageKeys.PAGE_INVALID);
        }
        if (scope != null && !scope.isBlank()) {
            ValidationResult result = new ValidationResult();
            validateScope(scope, result);
            reject(result);
        }
    }

    private void validateScope(String scope, ValidationResult result) {
        if (scope == null || !scopeService.existsActive(scope)) result.addError(
            "scope",
            PublisherMessageKeys.SCOPE_INVALID
        );
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
