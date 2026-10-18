package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeEnum;

import org.springframework.data.domain.Pageable;

import java.util.Locale;
import java.util.Set;

public final class ParameterKeyQueryValidation {
    private static final Set<String> SORTABLE =
            Set.of(
                    "id",
                    "identifier",
                    "key",
                    "description",
                    "type",
                    "status",
                    "active",
                    "version",
                    "createdAt",
                    "updatedAt");

    private ParameterKeyQueryValidation() {}

    public static void validatePage(Pageable page) {
        if (page.isUnpaged()
                || page.getOffset() > Integer.MAX_VALUE
                || page.getSort().stream()
                        .anyMatch(order -> !SORTABLE.contains(order.getProperty()))) {
            throw new ValidationException(ParameterKeyMessageKeys.PAGE_INVALID);
        }
    }

    public static String resolveType(String value) {
        return resolve(value, ParameterKeyTypeEnum.class);
    }

    public static String resolveStatus(String value) {
        return resolve(value, ParameterKeyStatusTypeEnum.class);
    }

    private static <E extends Enum<E>> String resolve(String value, Class<E> type) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ParameterKeyMessageKeys.FILTER_INVALID);
        }
    }
}
