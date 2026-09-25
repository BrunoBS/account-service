package br.com.itau.portalmanager.workspace.feature.platform.domain.support;

import java.util.regex.Pattern;

public final class PlatformCodeValidator {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private PlatformCodeValidator() {
    }

    public static String requireValid(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Platform code is required");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Platform code must contain only uppercase letters, numbers and underscore");
        }
        return code;
    }
}
