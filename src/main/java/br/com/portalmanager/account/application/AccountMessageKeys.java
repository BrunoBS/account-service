package br.com.portalmanager.account.application;

public final class AccountMessageKeys {

    private static final String PREFIX = "account-service.";

    public static final String NOT_FOUND = PREFIX + "account.not-found";
    public static final String RESTORE_INVALID = PREFIX + "account.restore.invalid";
    public static final String ACCOUNT_TYPE_INVALID = PREFIX + "validation.account-type.invalid";
    public static final String NAME_REQUIRED = PREFIX + "validation.name.required";
    public static final String NAME_SIZE = PREFIX + "validation.name.size";
    public static final String NAME_DUPLICATE = PREFIX + "validation.name.duplicate";
    public static final String DESCRIPTION_SIZE = PREFIX + "validation.description.size";
    public static final String REQUESTER_SIZE = PREFIX + "validation.requester.size";
    public static final String ACRONYM_REQUIRED = PREFIX + "validation.acronym.required";
    public static final String ACRONYM_SIZE = PREFIX + "validation.acronym.size";
    public static final String EMAIL_INVALID = PREFIX + "validation.email.invalid";
    public static final String APPROVERS_REQUIRED = PREFIX + "validation.approvers.required";
    public static final String APPROVER_FUNCTIONAL_REQUIRED = PREFIX + "validation.approver.functional.required";
    public static final String VERSION_REQUIRED = PREFIX + "validation.version.required";
    public static final String TYPE_FILTER_INVALID = PREFIX + "validation.type-filter.invalid";

    private AccountMessageKeys() {
    }
}
