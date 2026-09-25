package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain;

import br.com.portalmanager.platform.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class FeatureTypeCode extends AbstractCatalogCode {

    protected FeatureTypeCode() {
    }

    private FeatureTypeCode(String value) {
        super(value);
    }

    private FeatureTypeCode(FeatureTypeEnum value) {
        super(value);
    }

    public static FeatureTypeCode of(String value) {
        return new FeatureTypeCode(value);
    }

    public static FeatureTypeCode of(FeatureTypeEnum value) {
        return new FeatureTypeCode(value);
    }
}
