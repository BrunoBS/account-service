package br.com.portalmanager.platform.workspace.feature.platform.domain.support;

public final class PlatformNameValidator {

    private PlatformNameValidator() {
    }

    public static String requireValid(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Platform name is required");
        }
        return name;
    }
}
