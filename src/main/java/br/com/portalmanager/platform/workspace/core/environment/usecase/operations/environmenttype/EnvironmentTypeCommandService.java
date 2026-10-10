package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeCommandService {

    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;
    private final EnvironmentTypeQueryService query;
    private final EnvironmentTypeValidator validator;

    public EnvironmentTypeCommandService(
        EnvironmentTypeRepository types,
        EnvironmentTypeCompatibilityRepository compatibilities,
        EnvironmentRepository environments,
        EnvironmentTypeQueryService query,
        EnvironmentTypeValidator validator
    ) {
        this.types = types;
        this.compatibilities = compatibilities;
        this.environments = environments;
        this.query = query;
        this.validator = validator;
    }

    @Transactional
    public EnvironmentTypeOutput create(EnvironmentTypeInput input) {
        boolean duplicateCode = input != null && input.code() != null && types.existsByCode(input.code().trim());
        validator.validateForCreate(input, duplicateCode);
        String code = input.code().trim();
        EnvironmentType type = new EnvironmentType(
            code,
            input.name().trim(),
            input.description().trim(),
            input.rootAllowed(),
            input.workspaceRequired(),
            input.displayOrder(),
            LocalDateTime.now()
        );
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    public EnvironmentTypeOutput update(String identifier, EnvironmentTypeInput input) {
        EnvironmentType type = query.find(identifier);
        validator.validateForUpdate(input);
        if (!Objects.equals(type.getVersion(), input.version())) throw new ResourceVersionConflictException();
        boolean codeChanged = !Objects.equals(type.getCode(), input.code().trim());
        boolean scopeInUse =
            type.isWorkspaceRequired() != input.workspaceRequired() &&
            environments.existsByEnvironmentTypeId(type.getId());
        boolean rootInUse =
            type.isRootAllowed() &&
            !input.rootAllowed() &&
            environments.existsByEnvironmentTypeIdAndParentIsNull(type.getId());
        validator.validateUpdateConflicts(codeChanged, scopeInUse, rootInUse);
        type.update(
            input.name().trim(),
            input.description().trim(),
            input.rootAllowed(),
            input.workspaceRequired(),
            input.displayOrder(),
            LocalDateTime.now()
        );
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    public void inactivate(String identifier) {
        EnvironmentType type = query.find(identifier);
        validator.validateForInactivate(environments.existsByEnvironmentTypeId(type.getId()));
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
        boolean inactive = LifecycleTypeCode.inactive().equals(type.getLifecycle());
        boolean inUse =
            environments.existsByEnvironmentTypeId(type.getId()) ||
            compatibilities.existsByParentTypeIdOrChildTypeId(type.getId(), type.getId());
        validator.validateForDelete(inactive, inUse);
        types.delete(type);
    }
}
