package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EnvironmentTypeCompatibilityQueryService {
    private static final String LIFECYCLE_FIELD = "lifecycle";
    private final EnvironmentTypeCompatibilityRepository repository;

    public EnvironmentTypeCompatibilityQueryService(EnvironmentTypeCompatibilityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentCompatibilityOutput> list(String lifecycle) {
        LifecycleTypeCode effectiveLifecycle = resolveLifecycle(lifecycle);
        return repository.findByLifecycle(effectiveLifecycle).stream()
                .map(EnvironmentCompatibilityOutput::from)
                .toList();
    }

    private LifecycleTypeCode resolveLifecycle(String lifecycle) {
        if (lifecycle == null || lifecycle.isBlank()) {
            return LifecycleTypeCode.active();
        }
        try {
            LifecycleTypeEnum value = LifecycleTypeEnum.valueOf(lifecycle.trim().toUpperCase(Locale.ROOT));
            return LifecycleTypeCode.of(value);
        } catch (IllegalArgumentException exception) {
            ValidationResult result = new ValidationResult();
            result.addError(LIFECYCLE_FIELD, "environment.compatibility.lifecycle-invalid");
            throw new ValidationException(result);
        }
    }
}
