package br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class PublicationModeTypeCode extends AbstractCatalogCode {

    protected PublicationModeTypeCode() {}

    private PublicationModeTypeCode(String value) {
        super(value);
    }

    public static PublicationModeTypeCode of(String value) {
        return of(requireEnumValue(value, PublicationModeTypeEnum.class));
    }

    public static PublicationModeTypeCode of(PublicationModeTypeEnum value) {
        return new PublicationModeTypeCode(value == null ? null : value.name());
    }
}
