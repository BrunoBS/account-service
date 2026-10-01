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
                input.version(),
                input.workspaceType(),
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
                toApproverData(input.approvers())
        );
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
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

}
