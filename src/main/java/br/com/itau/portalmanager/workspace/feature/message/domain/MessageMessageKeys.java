package br.com.itau.portalmanager.workspace.feature.message.domain;

public final class MessageMessageKeys {

    private static final String PREFIX = "workspace-service.";

    public static final String NOT_FOUND = PREFIX + "message.not-found";
    public static final String DELETE_INVALID = PREFIX + "message.delete.invalid";
    public static final String TRANSLATION_NOT_FOUND = PREFIX + "message.translation.not-found";
    public static final String TRANSLATION_DELETE_INVALID =
            PREFIX + "message.translation.delete.invalid";

    public static final String SERVICE_REQUIRED = PREFIX + "validation.message.service.required";
    public static final String SERVICE_INVALID = PREFIX + "validation.message.service.invalid";
    public static final String KEY_REQUIRED = PREFIX + "validation.message.key.required";
    public static final String KEY_INVALID = PREFIX + "validation.message.key.invalid";
    public static final String KEY_DUPLICATE = PREFIX + "validation.message.key.duplicate";
    public static final String CODE_REQUIRED = PREFIX + "validation.message.code.required";
    public static final String CODE_INVALID = PREFIX + "validation.message.code.invalid";
    public static final String CODE_DUPLICATE = PREFIX + "validation.message.code.duplicate";
    public static final String HTTP_STATUS_INVALID = PREFIX + "validation.message.http-status.invalid";
    public static final String OBSERVATION_SIZE = PREFIX + "validation.message.observation.size";
    public static final String VERSION_REQUIRED = PREFIX + "validation.message.version.required";
    public static final String LOCALE_INVALID = PREFIX + "validation.message.translation.locale.invalid";
    public static final String LOCALE_DUPLICATE = PREFIX + "validation.message.translation.locale.duplicate";
    public static final String TITLE_REQUIRED = PREFIX + "validation.message.translation.title.required";
    public static final String DETAIL_REQUIRED = PREFIX + "validation.message.translation.detail.required";
    public static final String SUGGESTION_REQUIRED =
            PREFIX + "validation.message.translation.suggestion.required";

    private MessageMessageKeys() {
    }
}
