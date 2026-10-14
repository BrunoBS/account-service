package br.com.portalmanager.platform.workspace.core.workspace.usecase.validation;

import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceValidator {

    private final WorkspaceTypeService workspaceTypeService;
    private final WorkspaceRepository repository;

    public WorkspaceValidator(WorkspaceTypeService workspaceTypeService, WorkspaceRepository repository) {
        this.workspaceTypeService = workspaceTypeService;
        this.repository = repository;
    }

    public static void requireActive(Workspace workspace) {
        if (!LifecycleTypeCode.active().equals(workspace.getLifecycle())) {
            throw new NotFoundException(WorkspaceMessageKeys.NOT_FOUND);
        }
    }

    private static void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    private void validateCreationPermissions(CreateWorkspaceInput input) {
        boolean authorizerGroupProvided = input.authorizerGroup() != null && !input.authorizerGroup().isBlank();
        if (authorizerGroupProvided || WorkspaceTypeEnum.ADMIN.name().equals(input.workspaceType())) {
            requireOwner();
        }
    }

    private void requireOwner() {
        if (!UserContext.get().map(UserSession::isOwner).orElse(false)) {
            throw new ForbiddenAccessException(WorkspaceMessageKeys.OWNER_REQUIRED);
        }
    }

    private void validateSensitiveChanges(Workspace workspace, UpdateWorkspaceInput input) {
        boolean typeChanged = !Objects.equals(workspace.getWorkspaceType().value(), input.workspaceType());
        boolean groupChanged = !Objects.equals(workspace.getAuthorizerGroup(), input.authorizerGroup());
        if (typeChanged || groupChanged) requireOwner();
        if (typeChanged && repository.findAssociatedApplicationId(workspace.getId()).isPresent()) {
            ValidationResult result = new ValidationResult();
            result.addError("workspaceType", WorkspaceMessageKeys.TYPE_CHANGE_WITH_APPLICATIONS);
            rejectIfInvalid(result);
        }
    }

    private void requireUpdateVersion(Long current, Long requested) {
        if (requested == null) {
            ValidationResult result = new ValidationResult();
            result.addError("version", WorkspaceMessageKeys.VERSION_REQUIRED);
            rejectIfInvalid(result);
        }
        requireVersion(current, requested);
    }

    public void validatePagination(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getOffset() > Integer.MAX_VALUE) {
            throw new ValidationException(WorkspaceMessageKeys.PAGE_INVALID);
        }
        Set<String> sortableProperties = Set.of(
            "id",
            "identifier",
            "name",
            "description",
            "requester",
            "acronym",
            "emailGroup",
            "onboarding",
            "createdAt",
            "updatedAt"
        );
        if (
            pageable
                .getSort()
                .stream()
                .anyMatch(order -> !sortableProperties.contains(order.getProperty()))
        ) {
            throw new ValidationException(WorkspaceMessageKeys.PAGE_INVALID);
        }
    }

    public void validateForCreate(CreateWorkspaceInput input) {
        if (input.version() != null) {
            ValidationResult result = new ValidationResult();
            result.addError("version", WorkspaceMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
            rejectIfInvalid(result);
        }
        validateCreationPermissions(input);
        boolean nameDuplicate = repository.existsByName(input.name());
        validateBusiness(input.workspaceType(), input.approvers(), nameDuplicate);
    }

    public void validateForUpdate(Workspace workspace, UpdateWorkspaceInput input) {
        requireUpdateVersion(workspace.getVersion(), input.version());
        validateSensitiveChanges(workspace, input);
        boolean nameChanged = !workspace.getName().equals(input.name());
        boolean nameDuplicate = nameChanged && repository.existsByNameAndIdNot(input.name(), workspace.getId());
        validateBusiness(input.workspaceType(), input.approvers(), nameDuplicate);
    }

    public void validateTypeFilter(String normalizedTypeName) {
        if (normalizedTypeName == null) return;
        ValidationResult result = new ValidationResult();
        if (!isSupportedType(normalizedTypeName) || !workspaceTypeService.existsActive(normalizedTypeName)) {
            result.addError("typeName", WorkspaceMessageKeys.TYPE_FILTER_INVALID);
        }
        rejectIfInvalid(result);
    }

    private void validateBusiness(String workspaceType, List<ApproverInput> approvers, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();
        if (!isSupportedType(workspaceType) || !workspaceTypeService.existsActive(workspaceType)) {
            result.addError("workspaceType", WorkspaceMessageKeys.WORKSPACE_TYPE_INVALID);
        }
        if (nameDuplicate) result.addError("name", WorkspaceMessageKeys.NAME_DUPLICATE);
        Set<String> functionals = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (int index = 0; index < approvers.size(); index++) {
            ApproverInput approver = approvers.get(index);
            String path = "approvers[" + index + "]";
            if (!functionals.add(approver.functional().toUpperCase(Locale.ROOT))) {
                result.addError(path + ".functional", WorkspaceMessageKeys.APPROVER_FUNCTIONAL_DUPLICATE);
            }
            if (!emails.add(approver.email().toLowerCase(Locale.ROOT))) {
                result.addError(path + ".email", WorkspaceMessageKeys.APPROVER_EMAIL_DUPLICATE);
            }
        }
        rejectIfInvalid(result);
    }

    private boolean isSupportedType(String code) {
        return java.util.Arrays.stream(WorkspaceTypeEnum.values()).anyMatch(type -> type.name().equals(code));
    }

    private void rejectIfInvalid(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
