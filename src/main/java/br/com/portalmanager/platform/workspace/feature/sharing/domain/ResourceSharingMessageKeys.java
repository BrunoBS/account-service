package br.com.portalmanager.platform.workspace.feature.sharing.domain;

public final class ResourceSharingMessageKeys {

    public static final String NOT_FOUND = "resource-sharing.not-found";
    public static final String REQUEST_INVALID = "resource-sharing.request.invalid";
    public static final String DUPLICATE = "resource-sharing.contract.duplicate";
    public static final String DELETE_REQUIRES_INACTIVE =
            "resource-sharing.contract.delete-requires-inactive";
    public static final String LIFECYCLE_INVALID = "resource-sharing.contract.lifecycle-invalid";
    public static final String STATE_INVALID = "resource-sharing.participant.state-invalid";
    public static final String MAPPING_INVALID = "resource-sharing.mapping.invalid";
    public static final String SCOPE_INVALID = "resource-sharing.scope.invalid";
    public static final String PUBLICATION_MODE_INVALID =
            "resource-sharing.publication-mode.invalid";

    public static final String FEATURE_REQUIRED = "resource-sharing.feature.required";
    public static final String FEATURE_IMMUTABLE = "resource-sharing.feature.immutable";
    public static final String FEATURE_UNAVAILABLE =
            "resource-sharing.contract.feature-unavailable";

    public static final String OWNER_APPLICATION_TYPE_INVALID =
            "resource-sharing.contract.owner-application-type-invalid";

    private ResourceSharingMessageKeys() {}
}
