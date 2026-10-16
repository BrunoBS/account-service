package br.com.portalmanager.platform.workspace.feature.segmentation.domain;

public final class SegmentationMessageKeys {

    public static final String NOT_FOUND = "segmentation.not-found";
    public static final String CATEGORY_NOT_FOUND = "segmentation.category.not-found";
    public static final String CODE_DUPLICATE = "segmentation.code.duplicate";
    public static final String CATEGORY_CODE_DUPLICATE = "segmentation.category.code.duplicate";
    public static final String CODE_IMMUTABLE = "segmentation.code.immutable";
    public static final String PARENT_INACTIVE = "segmentation.category.parent.inactive";
    public static final String ACTIVATE_INVALID = "segmentation.activate.invalid";
    public static final String INACTIVATE_INVALID = "segmentation.inactivate.invalid";
    public static final String CATEGORY_ACTIVATE_INVALID = "segmentation.category.activate.invalid";
    public static final String CATEGORY_INACTIVATE_INVALID =
            "segmentation.category.inactivate.invalid";
    public static final String VERSION_REQUIRED = "segmentation.version.required";
    public static final String VERSION_NOT_ALLOWED_ON_CREATE =
            "segmentation.version.not-allowed-on-create";
    public static final String PAGE_INVALID = "segmentation.page.invalid";
    public static final String LIFECYCLE_INVALID = "segmentation.lifecycle.invalid";
    public static final String REQUEST_REQUIRED = "segmentation.request.required";

    private SegmentationMessageKeys() {}
}
