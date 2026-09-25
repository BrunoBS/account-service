package br.com.itau.portalmanager.workspace.feature.platform.domain.support;

import java.util.regex.Pattern;

public final class PlatformNameValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private PlatformNameValidator() {
    }

    public static String requireValid(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Platform name is required");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Platform name must contain only uppercase letters, numbers and underscore");
        }
        return name;
    }
}
