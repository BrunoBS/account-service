package br.com.itau.portalmanager.workspace.core.workspace.domain;

import java.util.regex.Pattern;

final class SemanticCode {

    private static final Pattern CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    private SemanticCode() {
    }

    static String requireValid(String value, String type) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid " + type + " code: " + value);
        }
        return value;
    }

    static boolean isValid(String value) {
        return value != null && CODE_PATTERN.matcher(value).matches();
    }
}
