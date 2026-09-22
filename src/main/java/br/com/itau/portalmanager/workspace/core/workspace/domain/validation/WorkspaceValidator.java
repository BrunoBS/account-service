package br.com.itau.portalmanager.workspace.core.workspace.domain.validation;

import br.com.itau.portalmanager.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceTypeEnum;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class WorkspaceValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void validateForCreate(WorkspaceValidationData data, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();

        if (data == null) {
            result.addError("request", WorkspaceMessageKeys.NAME_REQUIRED);
            rejectIfInvalid(result);
            return;
        }

        validateCommon(data, result);

        if (nameDuplicate) {
            result.addError("name", WorkspaceMessageKeys.NAME_DUPLICATE);
        }

        rejectIfInvalid(result);
    }

    public void validateForUpdate(WorkspaceValidationData data, boolean nameDuplicate) {
        ValidationResult result = new ValidationResult();

        if (data == null) {
            result.addError("request", WorkspaceMessageKeys.NAME_REQUIRED);
            rejectIfInvalid(result);
            return;
        }

        if (data.version() == null || data.version() < 0) {
            result.addError("version", WorkspaceMessageKeys.VERSION_REQUIRED);
        }

        validateCommon(data, result);

        if (nameDuplicate) {
            result.addError("name", WorkspaceMessageKeys.NAME_DUPLICATE);
        }

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

    private void validateCommon(WorkspaceValidationData data, ValidationResult result) {
        if (!isValidWorkspaceType(data.workspaceType())) {
            result.addError("workspaceType", WorkspaceMessageKeys.WORKSPACE_TYPE_INVALID);
        }

        if (data.name() == null || data.name().isBlank()) {
            result.addError("name", WorkspaceMessageKeys.NAME_REQUIRED);
        } else if (data.name().length() < 3 || data.name().length() > 100) {
            result.addError("name", WorkspaceMessageKeys.NAME_SIZE);
        }

        if (data.description() == null || data.description().isBlank()
                || data.description().length() < 10 || data.description().length() > 500) {
            result.addError("description", WorkspaceMessageKeys.DESCRIPTION_SIZE);
        }

        if (data.requester() == null || data.requester().isBlank() || data.requester().length() < 5) {
            result.addError("requester", WorkspaceMessageKeys.REQUESTER_SIZE);
        }

        if (data.acronym() == null || data.acronym().isBlank()) {
            result.addError("acronym", WorkspaceMessageKeys.ACRONYM_REQUIRED);
        } else if (data.acronym().length() > 5) {
            result.addError("acronym", WorkspaceMessageKeys.ACRONYM_SIZE);
        }

        if (!isEmail(data.emailGroup())) {
            result.addError("emailGroup", WorkspaceMessageKeys.EMAIL_INVALID);
        }

        validateApprovers(data.approvers(), result);
    }

    private void validateApprovers(List<ApproverData> approvers, ValidationResult result) {
        if (approvers == null || approvers.isEmpty()) {
            result.addError("approvers", WorkspaceMessageKeys.APPROVERS_REQUIRED);
            return;
        }

        for (int index = 0; index < approvers.size(); index++) {
            ApproverData approver = approvers.get(index);
            String path = "approvers[" + index + "]";

            if (approver == null) {
                result.addError(path, WorkspaceMessageKeys.APPROVERS_REQUIRED);
                continue;
            }

            if (approver.functional() == null || approver.functional().isBlank()) {
                result.addError(path + ".functional", WorkspaceMessageKeys.APPROVER_FUNCTIONAL_REQUIRED);
            }

            if (!isEmail(approver.email())) {
                result.addError(path + ".email", WorkspaceMessageKeys.EMAIL_INVALID);
            }
        }
    }

    private boolean isValidWorkspaceType(String value) {
        if (value == null) {
            return false;
        }

        try {
            WorkspaceTypeEnum.valueOf(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
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
