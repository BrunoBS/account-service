package br.com.itau.portalmanager.workspace.core.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class LifecycleTypeCode {

    private static final String ACTIVE_CODE = "ACTIVE";
    private static final String INACTIVE_CODE = "INACTIVE";
    private static final String PENDING_DELETION_CODE = "PENDING_DELETION";

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    @Column(name = "lifecycle_code", nullable = false, length = 50)
    private String value;

    protected LifecycleTypeCode() {
    }

    private LifecycleTypeCode(String value) {
        if (value == null || !CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid lifecycle type code: " + value);
        }
        this.value = value;
    }

    public static LifecycleTypeCode of(String value) {
        return new LifecycleTypeCode(value);
    }

    public static LifecycleTypeCode active() {
        return of(ACTIVE_CODE);
    }

    public static LifecycleTypeCode inactive() {
        return of(INACTIVE_CODE);
    }

    public static LifecycleTypeCode pendingDeletion() {
        return of(PENDING_DELETION_CODE);
    }

    public String value() {
        return value;
    }

    public static boolean isValidFormat(String value) {
        return value != null && CODE_PATTERN.matcher(value).matches();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LifecycleTypeCode that)) return false;
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
