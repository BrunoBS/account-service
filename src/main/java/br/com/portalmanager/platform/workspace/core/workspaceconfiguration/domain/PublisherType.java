package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;

public enum PublisherType {
    WEB_SOCKET("environment-configuration-publisher-web-socket"),
    KAFKA("environment-configuration-publisher-kafka"),
    APP_CONFIG("environment-configuration-publisher-app-config");

    private final String schemaCode;

    PublisherType(String schemaCode) {
        this.schemaCode = schemaCode;
    }

    public String schemaCode() {
        return schemaCode;
    }

    public static PublisherType fromCode(String code) {
        for (PublisherType type : values()) {
            if (type.name().equals(code)) return type;
        }
        throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED);
    }
}
