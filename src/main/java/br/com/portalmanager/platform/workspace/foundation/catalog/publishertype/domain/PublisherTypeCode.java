package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;

import jakarta.persistence.Embeddable;

@Embeddable
public class PublisherTypeCode extends AbstractCatalogCode {
    protected PublisherTypeCode() {}

    private PublisherTypeCode(String value) {
        super(value);
    }

    public static PublisherTypeCode of(String value) {
        return of(requireEnumValue(value, PublisherTypeEnum.class));
    }

    public static PublisherTypeCode of(PublisherTypeEnum value) {
        return new PublisherTypeCode(value == null ? null : value.name());
    }

    public static PublisherTypeCode webSocket() {
        return of(PublisherTypeEnum.WEB_SOCKET);
    }

    public static PublisherTypeCode kafka() {
        return of(PublisherTypeEnum.KAFKA);
    }

    public static PublisherTypeCode appConfig() {
        return of(PublisherTypeEnum.APP_CONFIG);
    }
}
