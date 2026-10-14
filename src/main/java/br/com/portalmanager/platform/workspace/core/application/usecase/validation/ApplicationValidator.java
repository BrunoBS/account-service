package br.com.portalmanager.platform.workspace.core.application.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationSharingReferencePort;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ApplicationValidator {

    private final ApplicationScopeTypeService scopeService;
    private final ApplicationRepository repository;
    private final ApplicationSharingReferencePort sharingReferences;

    public ApplicationValidator(
        ApplicationScopeTypeService scopeService,
        ApplicationRepository repository,
        ApplicationSharingReferencePort sharingReferences
    ) {
        this.scopeService = scopeService;
        this.repository = repository;
        this.sharingReferences = sharingReferences;
    }

    public void validateForCreate(String workspaceType, Long workspaceId, CreateApplicationInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", ApplicationMessageKeys.NAME_INVALID);
            reject(result);
            return;
        }

        if (input.version() != null) {
            result.addError("version", ApplicationMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
            reject(result);
        }

        validateBusinessRules(
            workspaceType,
            input.applicationScope(),
            repository.existsByWorkspaceIdAndName(workspaceId, input.name()),
            result
        );
        reject(result);
    }

    public void validateForUpdate(String workspaceType, Application application, UpdateApplicationInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", ApplicationMessageKeys.NAME_INVALID);
            reject(result);
            return;
        }

        if (input.version() == null) {
            result.addError("version", ApplicationMessageKeys.VERSION_REQUIRED);
        }
        validateBusinessRules(
            workspaceType,
            input.applicationScope(),
            !Objects.equals(application.getName(), input.name()) &&
                repository.existsByWorkspaceIdAndNameAndIdNot(
                    application.getWorkspaceId(),
                    input.name(),
                    application.getId()
                ),
            result
        );
        reject(result);
        requireVersion(application.getVersion(), input.version());
        if (
            !ApplicationScopeTypeCode.shared().value().equals(input.applicationScope()) &&
            !application.getApplicationScope().value().equals(input.applicationScope()) &&
            sharingReferences.hasSharedContracts(application.getId())
        ) {
            throw new ValidationException(ApplicationMessageKeys.HAS_SHARED_CONTRACTS);
        }
    }

    public void validatePagination(Pageable pageable) {
        Set<String> sortableProperties = Set.of(
            "id",
            "identifier",
            "name",
            "alias",
            "acronym",
            "createdAt",
            "updatedAt"
        );
        if (
            pageable.isUnpaged() ||
            pageable.getOffset() > Integer.MAX_VALUE ||
            pageable
                .getSort()
                .stream()
                .anyMatch(order -> !sortableProperties.contains(order.getProperty()))
        ) {
            throw new ValidationException(ApplicationMessageKeys.PAGE_INVALID);
        }
    }

    private void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    private void validateBusinessRules(String workspaceType, String scope, boolean duplicate, ValidationResult result) {
        if (!WorkspaceTypeCode.manager().value().equals(workspaceType)) {
            result.addError("workspaceIdentifier", ApplicationMessageKeys.WORKSPACE_TYPE_INVALID);
        }
        if (duplicate) {
            result.addError("name", ApplicationMessageKeys.NAME_DUPLICATE);
        }
        if (scope != null && !scopeService.existsActive(scope)) {
            result.addError("applicationScope", ApplicationMessageKeys.SCOPE_INVALID);
        }
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
