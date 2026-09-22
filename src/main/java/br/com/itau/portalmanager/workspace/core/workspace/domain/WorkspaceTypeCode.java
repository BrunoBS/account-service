package br.com.itau.portalmanager.workspace.core.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class WorkspaceTypeCode {

    @Column(name = "workspace_type_code", nullable = false, length = 50)
    private String value;

    protected WorkspaceTypeCode() {
    }

    private WorkspaceTypeCode(String value) {
        this.value = SemanticCode.requireValid(value, "workspace type");
    }

    public static WorkspaceTypeCode of(String value) {
        return new WorkspaceTypeCode(value);
    }

    public String value() {
        return value;
    }

    public static boolean isValidFormat(String value) {
        return SemanticCode.isValid(value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceTypeCode that)) {
            return false;
        }
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
