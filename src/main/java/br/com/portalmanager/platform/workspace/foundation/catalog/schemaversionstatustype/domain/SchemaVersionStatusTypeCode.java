package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain;

import br.com.portalmanager.platform.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class SchemaVersionStatusTypeCode extends AbstractCatalogCode {

    protected SchemaVersionStatusTypeCode() {
    }

    private SchemaVersionStatusTypeCode(String value) {
        super(value);
    }

    private SchemaVersionStatusTypeCode(SchemaVersionStatusTypeEnum value) {
        super(value);
    }

    public static SchemaVersionStatusTypeCode of(String value) {
        return new SchemaVersionStatusTypeCode(value);
    }

    public static SchemaVersionStatusTypeCode of(SchemaVersionStatusTypeEnum value) {
        return new SchemaVersionStatusTypeCode(value);
    }

    public static SchemaVersionStatusTypeCode draft() {
        return of(SchemaVersionStatusTypeEnum.DRAFT);
    }

    public static SchemaVersionStatusTypeCode published() {
        return of(SchemaVersionStatusTypeEnum.PUBLISHED);
    }
}
