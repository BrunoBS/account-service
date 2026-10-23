package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;

import java.util.Locale;

/** Common lifecycle filter for public resource listings. */
public final class LifecycleFilter {

    private LifecycleFilter() {}

    public static LifecycleTypeCode resolve(String lifecycle) {
        if (lifecycle == null) return LifecycleTypeCode.active();
        String normalizedLifecycle = lifecycle.trim().toUpperCase(Locale.ROOT);
        if (normalizedLifecycle.isEmpty())
            throw new ValidationException(LifecycleMessageKeys.FILTER_INVALID);
        try {
            return LifecycleTypeCode.of(LifecycleTypeEnum.valueOf(normalizedLifecycle));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(LifecycleMessageKeys.FILTER_INVALID);
        }
    }
}
