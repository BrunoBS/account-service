package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class EnvironmentTypeCommandService {
    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;
    private final EnvironmentTypeQueryService query;
    private final EnvironmentTypeValidator validator;

    public EnvironmentTypeCommandService(EnvironmentTypeRepository types,
                                         EnvironmentTypeCompatibilityRepository compatibilities,
                                         EnvironmentRepository environments, EnvironmentTypeQueryService query,
                                         EnvironmentTypeValidator validator) {
        this.types = types;
        this.compatibilities = compatibilities;
        this.environments = environments;
        this.query = query;
        this.validator = validator;
    }

    @Transactional
    public EnvironmentTypeOutput create(EnvironmentTypeInput input) {
        validator.validate(input, true);
        String code = input.code().trim();
        if (types.existsByCode(code)) throw new ValidationException(EnvironmentMessageKeys.TYPE_DUPLICATE);
        EnvironmentType type = new EnvironmentType(code, input.name().trim(), input.description().trim(),
                input.rootAllowed(), input.workspaceRequired(), input.displayOrder(), LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    public EnvironmentTypeOutput update(String identifier, EnvironmentTypeInput input) {
        EnvironmentType type = query.find(identifier);
        validator.validate(input, false);
        if (!Objects.equals(type.getVersion(), input.version())) throw new ResourceVersionConflictException();
        if (!Objects.equals(type.getCode(), input.code().trim()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_CODE_IMMUTABLE);
        if (type.isWorkspaceRequired() != input.workspaceRequired() && environments.existsByEnvironmentTypeId(type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        if (type.isRootAllowed() && !input.rootAllowed() && environments.existsByEnvironmentTypeIdAndParentIsNull(type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        type.update(input.name().trim(), input.description().trim(), input.rootAllowed(), input.workspaceRequired(),
                input.displayOrder(), LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    public void inactivate(String identifier) {
        EnvironmentType type = query.find(identifier);
        if (environments.existsByEnvironmentTypeId(type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        type.inactivate(LocalDateTime.now());
    }

    @Transactional
    public EnvironmentTypeOutput restore(String identifier) {
        EnvironmentType type = query.find(identifier);
        type.restore(LocalDateTime.now());
        return EnvironmentTypeOutput.from(type);
    }

    @Transactional
    public void delete(String identifier) {
        EnvironmentType type = query.find(identifier);
        if (!LifecycleTypeCode.inactive().equals(type.getLifecycle())
                || environments.existsByEnvironmentTypeId(type.getId())
                || compatibilities.existsByParentTypeIdOrChildTypeId(type.getId(), type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        types.delete(type);
    }
}
