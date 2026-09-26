package br.com.portalmanager.platform.workspace.feature.platform.domain.support;

import java.util.regex.Pattern;

public final class PlatformCodeValidator {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    private PlatformCodeValidator() {
    }

    public static String requireValid(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Platform code is required");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Platform code must use lowercase kebab-case");
        }
        return code;
    }
}
