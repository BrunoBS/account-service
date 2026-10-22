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
}
