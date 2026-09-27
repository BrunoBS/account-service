package br.com.portalmanager.platform.workspace.foundation.schema.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class SchemaTypeCode {

    @Column(name = "value", nullable = false, length = 50)
    private String value;

    protected SchemaTypeCode() {
    }

    private SchemaTypeCode(String value) {
        this.value = normalize(value);
    }

    public static SchemaTypeCode of(String value) {
        return new SchemaTypeCode(value);
    }

    public String value() {
        return value;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SchemaTypeCode that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
