package br.com.portalmanager.platform.workspace.feature.servicecontext.domain;

public final class ServiceContextMessageKeys {

    public static final String MICROSERVICE_NOT_FOUND = "service-context.microservice.not-found";
    public static final String ACTIVE_MICROSERVICE_NOT_FOUND =
            "service-context.microservice.active.not-found";
    public static final String FEATURE_NOT_FOUND = "service-context.feature.not-found";
    public static final String FEATURE_GROUP_NOT_FOUND = "service-context.feature-group.not-found";
    public static final String CODE_INVALID = "service-context.code.invalid";
    public static final String CODE_DUPLICATE = "service-context.code.duplicate";
    public static final String NAME_INVALID = "service-context.name.invalid";
    public static final String NAME_DUPLICATE = "service-context.name.duplicate";
    public static final String DESCRIPTION_INVALID = "service-context.description.invalid";
    public static final String MICROSERVICE_REQUIRED = "service-context.microservice.required";
    public static final String SETTINGS_REQUIRED = "service-context.settings.required";
    public static final String REQUEST_REQUIRED = "service-context.request.required";
    public static final String MICROSERVICE_INACTIVE = "service-context.microservice.inactive";
    public static final String FEATURE_GROUP_INACTIVE = "service-context.feature-group.inactive";
    public static final String FEATURE_GROUP_HAS_FEATURES = "service-context.feature-group.has-features";

    public static final String FEATURE_HAS_RESOURCE_SHARING_CONTRACTS =
            "service-context.feature.has-resource-sharing-contracts";

    public static final String FEATURE_NOT_SHAREABLE = "service-context.feature.not-shareable";

    public static final String VERSION_REQUIRED = "service-context.version.required";
    public static final String VERSION_NOT_ALLOWED_ON_CREATE =
            "service-context.version.not-allowed-on-create";
    public static final String CODE_IMMUTABLE = "service-context.code.immutable";
    public static final String MICROSERVICE_ACTIVATE_INVALID =
            "service-context.microservice.activate.invalid";
    public static final String MICROSERVICE_PAGE_INVALID =
            "service-context.microservice.page.invalid";
    public static final String MICROSERVICE_SETTINGS_INVALID =
            "service-context.microservice.settings.invalid";
    public static final String GLOBAL_MICROSERVICE_PROTECTED =
            "service-context.microservice.global.protected";

    public static final String FEATURE_ACTIVATE_INVALID =
            "service-context.feature.activate.invalid";
    public static final String FEATURE_PAGE_INVALID = "service-context.feature.page.invalid";
    public static final String FEATURE_SETTINGS_INVALID =
            "service-context.feature.settings.invalid";

    public static final String FEATURE_GROUP_ACTIVATE_INVALID = "service-context.feature-group.activate.invalid";
    public static final String FEATURE_GROUP_PAGE_INVALID = "service-context.feature-group.page.invalid";

    private ServiceContextMessageKeys() {}
}
