package br.com.portalmanager.platform.workspace.core.workspace.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class WorkspaceValidator {

    private final WorkspaceTypeService workspaceTypeService;

    public WorkspaceValidator(WorkspaceTypeService workspaceTypeService) {
        this.workspaceTypeService = workspaceTypeService;
    }

    public static void requireActive(Workspace workspace) {
        if (!LifecycleTypeCode.active().equals(workspace.getLifecycle())) {
            throw new NotFoundException(WorkspaceMessageKeys.NOT_FOUND);
        }
    }

    public static void requireRestorable(Workspace workspace) {
        if (!LifecycleTypeCode.inactive().equals(workspace.getLifecycle())) {
            throw new ValidationException(WorkspaceMessageKeys.RESTORE_INVALID);
        }
    }

    public static void requireDeletable(Workspace workspace) {
        if (!LifecycleTypeCode.inactive().equals(workspace.getLifecycle())) {
            throw new ValidationException(WorkspaceMessageKeys.DELETE_INVALID);
        }
    }

    public static void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    public void validateForCreate(WorkspaceValidationData data, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();
        if (data == null || !Long.valueOf(0L).equals(data.version())) {
            result.addError("version", WorkspaceMessageKeys.VERSION_REQUIRED);
        }
        validateSemanticRules(data, nameDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateForUpdate(WorkspaceValidationData data, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();
        if (data == null) {
            result.addError("request", WorkspaceMessageKeys.NAME_REQUIRED);
            rejectIfInvalid(result);
            return;
        }
        validateSemanticRules(data, nameDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateTypeFilter(String normalizedTypeName) {
        if (normalizedTypeName == null) return;
        ValidationResult result = new ValidationResult();
        if (!isValidWorkspaceType(normalizedTypeName)) {
            result.addError("typeName", WorkspaceMessageKeys.TYPE_FILTER_INVALID);
        }
        rejectIfInvalid(result);
    }

    private void validateSemanticRules(WorkspaceValidationData data, boolean nameDuplicate, ValidationResult result) {
        if (data == null) {
            result.addError("request", WorkspaceMessageKeys.NAME_REQUIRED);
            return;
        }
        if (!isValidWorkspaceType(data.workspaceType())) {
            result.addError("workspaceType", WorkspaceMessageKeys.WORKSPACE_TYPE_INVALID);
        }
        if (nameDuplicate) {
            result.addError("name", WorkspaceMessageKeys.NAME_DUPLICATE);
        }
        validateApproverUniqueness(data.approvers(), result);
    }


    private void validateApproverUniqueness(List<ApproverData> approvers, ValidationResult result) {
        if (approvers == null) return;
        Set<String> functionals = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (int index = 0; index < approvers.size(); index++) {
            ApproverData approver = approvers.get(index);
            if (approver == null) continue;
            String path = "approvers[" + index + "]";
            if (approver.functional() != null
                    && !functionals.add(approver.functional().toUpperCase(Locale.ROOT))) {
                result.addError(path + ".functional", WorkspaceMessageKeys.APPROVER_FUNCTIONAL_DUPLICATE);
            }
            if (approver.email() != null
                    && !emails.add(approver.email().toLowerCase(Locale.ROOT))) {
                result.addError(path + ".email", WorkspaceMessageKeys.APPROVER_EMAIL_DUPLICATE);
            }
        }
    }

    private boolean isValidWorkspaceType(String value) {
        return WorkspaceTypeCode.isValidFormat(value) && workspaceTypeService.existsActive(value);
    }

    private void rejectIfInvalid(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
