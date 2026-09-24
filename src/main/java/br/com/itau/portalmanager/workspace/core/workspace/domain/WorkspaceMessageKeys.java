package br.com.itau.portalmanager.workspace.core.workspace.domain;

public final class WorkspaceMessageKeys {

    private static final String PREFIX = "workspace-service.";

    public static final String NOT_FOUND = PREFIX + "workspace.not-found";
    public static final String RESTORE_INVALID = PREFIX + "workspace.restore.invalid";
    public static final String DELETE_INVALID = PREFIX + "workspace.delete.invalid";
    public static final String WORKSPACE_TYPE_INVALID = PREFIX + "validation.workspace-type.invalid";
    public static final String NAME_REQUIRED = PREFIX + "validation.name.required";
    public static final String NAME_SIZE = PREFIX + "validation.name.size";
    public static final String NAME_DUPLICATE = PREFIX + "validation.name.duplicate";
    public static final String DESCRIPTION_SIZE = PREFIX + "validation.description.size";
    public static final String REQUESTER_SIZE = PREFIX + "validation.requester.size";
    public static final String ACRONYM_REQUIRED = PREFIX + "validation.acronym.required";
    public static final String ACRONYM_SIZE = PREFIX + "validation.acronym.size";
    public static final String EMAIL_INVALID = PREFIX + "validation.email.invalid";
    public static final String APPROVERS_REQUIRED = PREFIX + "validation.approvers.required";
    public static final String APPROVER_FUNCTIONAL_REQUIRED =
            PREFIX + "validation.approver.functional.required";
    public static final String VERSION_REQUIRED = PREFIX + "validation.version.required";
    public static final String TYPE_FILTER_INVALID = PREFIX + "validation.type-filter.invalid";
    public static final String APPROVER_FUNCTIONAL_DUPLICATE =
            PREFIX + "validation.approver.functional.duplicate";
    public static final String APPROVER_EMAIL_DUPLICATE =
            PREFIX + "validation.approver.email.duplicate";

    private WorkspaceMessageKeys() {
    }
}
