package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;

import org.springframework.data.domain.Pageable;

import java.util.Locale;
import java.util.Set;

/** Query rules shared by the two resources of the segment catalog. */
public final class SegmentQueryValidation {
    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "identifier", "code", "name", "version", "createdAt", "updatedAt");

    private SegmentQueryValidation() {}

    public static void validatePage(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(SegmentationMessageKeys.PAGE_INVALID);
        }
    }

    public static String resolveLifecycle(String lifecycle) {
        String normalized;
        try {
            normalized = LifecycleFilter.resolve(lifecycle).value();
        } catch (ValidationException exception) {
            throw new ValidationException(SegmentationMessageKeys.LIFECYCLE_INVALID);
        }
        if (!Set.of("ACTIVE", "INACTIVE").contains(normalized))
            throw new ValidationException(SegmentationMessageKeys.LIFECYCLE_INVALID);
        return normalized;
    }

    public static String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeName(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
