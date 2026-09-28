package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class TagOriginTypeCode extends AbstractCatalogCode {
    protected TagOriginTypeCode() {}

    private TagOriginTypeCode(String value) { super(value); }

    public static TagOriginTypeCode of(String value) { return new TagOriginTypeCode(value); }

    public static TagOriginTypeCode of(TagOriginTypeEnum value) { return of(value.name()); }
}
