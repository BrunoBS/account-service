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

import java.util.*;
import java.util.regex.Pattern;

@Component
public class WorkspaceValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
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

        if (!validateRequest(data, result)) {
            rejectIfInvalid(result);
            return;
        }

        validateCommon(data, result);
        validateDuplicateName(nameDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateForUpdate(WorkspaceValidationData data, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();

        if (!validateRequest(data, result)) {
            rejectIfInvalid(result);
            return;
        }

        validateVersion(data.version(), result);
        validateCommon(data, result);
        validateDuplicateName(nameDuplicate, result);
        rejectIfInvalid(result);
    }

    public void validateTypeFilter(String normalizedTypeName) {
        if (normalizedTypeName == null) {
            return;
        }

        ValidationResult result = new ValidationResult();
        if (!isValidWorkspaceType(normalizedTypeName)) {
            result.addError("typeName", WorkspaceMessageKeys.TYPE_FILTER_INVALID);
        }
        rejectIfInvalid(result);
    }

    private boolean validateRequest(WorkspaceValidationData data, ValidationResult result) {
        if (data != null) {
            return true;
        }

        result.addError("request", WorkspaceMessageKeys.NAME_REQUIRED);
        return false;
    }

    private void validateCommon(WorkspaceValidationData data, ValidationResult result) {
        validateWorkspaceType(data.workspaceType(), result);
        validateName(data.name(), result);
        validateDescription(data.description(), result);
        validateRequester(data.requester(), result);
        validateAcronym(data.acronym(), result);
        validateEmailGroup(data.emailGroup(), result);
        validateSettings(data.settings(), result);
        validateApprovers(data.approvers(), result);
    }

    private void validateWorkspaceType(String workspaceType, ValidationResult result) {
        if (!isValidWorkspaceType(workspaceType)) {
            result.addError("workspaceType", WorkspaceMessageKeys.WORKSPACE_TYPE_INVALID);
        }
    }

    private void validateName(String name, ValidationResult result) {
        if (name == null || name.isBlank()) {
            result.addError("name", WorkspaceMessageKeys.NAME_REQUIRED);
            return;
        }

        if (name.length() < 3 || name.length() > 100) {
            result.addError("name", WorkspaceMessageKeys.NAME_SIZE);
        }
    }

    private void validateDescription(String description, ValidationResult result) {
        if (description == null
                || description.isBlank()
                || description.length() < 10
                || description.length() > 500) {
            result.addError("description", WorkspaceMessageKeys.DESCRIPTION_SIZE);
        }
    }

    private void validateRequester(String requester, ValidationResult result) {
        if (requester == null || requester.isBlank() || requester.length() < 5) {
            result.addError("requester", WorkspaceMessageKeys.REQUESTER_SIZE);
        }
    }

    private void validateAcronym(String acronym, ValidationResult result) {
        if (acronym == null || acronym.isBlank()) {
            result.addError("acronym", WorkspaceMessageKeys.ACRONYM_REQUIRED);
            return;
        }

        if (acronym.length() > 5) {
            result.addError("acronym", WorkspaceMessageKeys.ACRONYM_SIZE);
        }
    }


    private void validateSettings(tools.jackson.databind.JsonNode settings, ValidationResult result) {
        if (settings != null && !settings.isObject()) {
            result.addError("settings", WorkspaceMessageKeys.SETTINGS_INVALID);
        }
    }

    private void validateEmailGroup(String emailGroup, ValidationResult result) {
        if (!isEmail(emailGroup)) {
            result.addError("emailGroup", WorkspaceMessageKeys.EMAIL_INVALID);
        }
    }

    private void validateVersion(Long version, ValidationResult result) {
        if (version == null || version < 0) {
            result.addError("version", WorkspaceMessageKeys.VERSION_REQUIRED);
        }
    }

    private void validateDuplicateName(boolean nameDuplicate, ValidationResult result) {
        if (nameDuplicate) {
            result.addError("name", WorkspaceMessageKeys.NAME_DUPLICATE);
        }
    }

    private void validateApprovers(List<ApproverData> approvers, ValidationResult result) {
        if (approvers == null || approvers.isEmpty()) {
            result.addError("approvers", WorkspaceMessageKeys.APPROVERS_REQUIRED);
            return;
        }

        Set<String> functionals = new HashSet<>();
        Set<String> emails = new HashSet<>();

        for (int index = 0; index < approvers.size(); index++) {
            validateApprover(approvers.get(index), index, functionals, emails, result);
        }
    }

    private void validateApprover(
            ApproverData approver,
            int index,
            Set<String> functionals,
            Set<String> emails,
            ValidationResult result
    ) {
        String path = "approvers[" + index + "]";

        if (approver == null) {
            result.addError(path, WorkspaceMessageKeys.APPROVERS_REQUIRED);
            return;
        }

        validateApproverFunctional(approver.functional(), path, functionals, result);
        validateApproverEmail(approver.email(), path, emails, result);
    }

    private void validateApproverFunctional(
            String functional,
            String path,
            Set<String> functionals,
            ValidationResult result
    ) {
        if (functional == null || functional.isBlank()) {
            result.addError(path + ".functional", WorkspaceMessageKeys.APPROVER_FUNCTIONAL_REQUIRED);
            return;
        }

        String normalizedFunctional = functional.toUpperCase(Locale.ROOT);
        if (!functionals.add(normalizedFunctional)) {
            result.addError(
                    path + ".functional",
                    WorkspaceMessageKeys.APPROVER_FUNCTIONAL_DUPLICATE
            );
        }
    }

    private void validateApproverEmail(
            String email,
            String path,
            Set<String> emails,
            ValidationResult result
    ) {
        if (!isEmail(email)) {
            result.addError(path + ".email", WorkspaceMessageKeys.EMAIL_INVALID);
            return;
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (!emails.add(normalizedEmail)) {
            result.addError(
                    path + ".email",
                    WorkspaceMessageKeys.APPROVER_EMAIL_DUPLICATE
            );
        }
    }

    private boolean isValidWorkspaceType(String value) {
        return WorkspaceTypeCode.isValidFormat(value)
                && workspaceTypeService.existsActive(value);
    }

    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    private void rejectIfInvalid(ValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
    }
}
