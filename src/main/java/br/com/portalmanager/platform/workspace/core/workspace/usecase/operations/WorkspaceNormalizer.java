package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.ApproverData;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidationData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class WorkspaceNormalizer {

    public CreateWorkspaceInput normalize(CreateWorkspaceInput input) {
        if (input == null) {
            return null;
        }

        return new CreateWorkspaceInput(
                normalizeType(input.workspaceType()),
                trim(input.name()),
                trim(input.description()),
                trim(input.requester()),
                trim(input.acronym()),
                trimOptional(input.authorizerGroup()),
                input.settings(),
                trim(input.emailGroup()),
                normalizeApprovers(input.approvers()),
                input.tags()
        );
    }

    public UpdateWorkspaceInput normalize(UpdateWorkspaceInput input) {
        if (input == null) {
            return null;
        }

        return new UpdateWorkspaceInput(
                input.version(),
                normalizeType(input.workspaceType()),
                trim(input.name()),
                trim(input.description()),
                trim(input.requester()),
                trim(input.acronym()),
                trimOptional(input.authorizerGroup()),
                input.settings(),
                trim(input.emailGroup()),
                normalizeApprovers(input.approvers()),
                input.tags()
        );
    }

    public String normalizeTypeFilter(String value) {
        return normalizeType(value);
    }

    public String normalizeTagFilter(String value) {
        return TagNormalizer.normalize(value);
    }

    public WorkspaceValidationData toValidationData(CreateWorkspaceInput input) {
        if (input == null) {
            return null;
        }
        return new WorkspaceValidationData(
                null,
                input.workspaceType(),
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.emailGroup(),
                toApproverData(input.approvers())
        );
    }

    public WorkspaceValidationData toValidationData(UpdateWorkspaceInput input) {
        if (input == null) {
            return null;
        }
        return new WorkspaceValidationData(
                input.version(),
                input.workspaceType(),
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.emailGroup(),
                toApproverData(input.approvers())
        );
    }

    private List<ApproverInput> normalizeApprovers(List<ApproverInput> approvers) {
        if (approvers == null) {
            return null;
        }

        return approvers.stream()
                .map(this::normalizeApprover)
                .toList();
    }

    private ApproverInput normalizeApprover(ApproverInput approver) {
        if (approver == null) {
            return null;
        }
        return new ApproverInput(trim(approver.functional()), trim(approver.email()));
    }

    private List<ApproverData> toApproverData(List<ApproverInput> approvers) {
        if (approvers == null) {
            return null;
        }
        return approvers.stream()
                .map(value -> value == null ? null : new ApproverData(value.functional(), value.email()))
                .toList();
    }

    private String normalizeType(String value) {
        String normalized = trimOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
