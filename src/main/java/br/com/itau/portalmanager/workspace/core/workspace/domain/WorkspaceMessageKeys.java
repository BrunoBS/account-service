package br.com.itau.portalmanager.workspace.core.workspace.domain;

public final class WorkspaceMessageKeys {

    public static final String NOT_FOUND = "workspace.not-found";
    public static final String RESTORE_INVALID = "workspace.restore.invalid";
    public static final String DELETE_INVALID = "workspace.delete.invalid";
    public static final String WORKSPACE_TYPE_INVALID = "validation.workspace-type.invalid";
    public static final String NAME_REQUIRED = "validation.name.required";
    public static final String NAME_SIZE = "validation.name.size";
    public static final String NAME_DUPLICATE = "validation.name.duplicate";
    public static final String DESCRIPTION_SIZE = "validation.description.size";
    public static final String REQUESTER_SIZE = "validation.requester.size";
    public static final String ACRONYM_REQUIRED = "validation.acronym.required";
    public static final String ACRONYM_SIZE = "validation.acronym.size";
    public static final String EMAIL_INVALID = "validation.email.invalid";
    public static final String APPROVERS_REQUIRED = "validation.approvers.required";
    public static final String APPROVER_FUNCTIONAL_REQUIRED =
            "validation.approver.functional.required";
    public static final String VERSION_REQUIRED = "validation.version.required";
    public static final String TYPE_FILTER_INVALID = "validation.type-filter.invalid";
    public static final String APPROVER_FUNCTIONAL_DUPLICATE =
            "validation.approver.functional.duplicate";
    public static final String APPROVER_EMAIL_DUPLICATE =
            "validation.approver.email.duplicate";

    private WorkspaceMessageKeys() {
    }
}
