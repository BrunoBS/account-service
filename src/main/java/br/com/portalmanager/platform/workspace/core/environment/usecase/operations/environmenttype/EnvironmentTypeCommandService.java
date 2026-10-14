package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.EnvironmentTypeMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeCommandService {

    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeFinder finder;
    private final EnvironmentTypeValidator validator;
    private final EnvironmentTypeMapper mapper;

    public EnvironmentTypeCommandService(
        EnvironmentTypeRepository types,
        EnvironmentTypeFinder finder,
        EnvironmentTypeValidator validator,
        EnvironmentTypeMapper mapper
    ) {
        this.types = types;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "ENVIRONMENT_TYPE_CREATED", resourceType = "ENVIRONMENT_TYPE")
    public EnvironmentTypeOutput create(EnvironmentTypeInput input) {
        validator.validateForCreate(input);
        EnvironmentTypeDetails details = mapper.toDetails(input);
        EnvironmentType type = new EnvironmentType(input.code(), details, LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "ENVIRONMENT_TYPE_UPDATED", resourceType = "ENVIRONMENT_TYPE")
    public EnvironmentTypeOutput update(String code, EnvironmentTypeInput input) {
        EnvironmentType type = finder.findForConfigurationUpdate(code);
        validator.validateForUpdate(type, input);
        EnvironmentTypeDetails details = mapper.toDetails(input);
        type.update(details, LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "ENVIRONMENT_TYPE_DEACTIVATED", resourceType = "ENVIRONMENT_TYPE")
    public EnvironmentTypeOutput inactivate(String code) {
        EnvironmentType type = finder.findForUpdate(code);
        type.inactivate(LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "ENVIRONMENT_TYPE_RESTORED", resourceType = "ENVIRONMENT_TYPE")
    public EnvironmentTypeOutput restore(String code) {
        EnvironmentType type = finder.findForUpdate(code);
        type.restore(LocalDateTime.now());
        return EnvironmentTypeOutput.from(types.saveAndFlush(type));
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "ENVIRONMENT_TYPE_PURGED", resourceType = "ENVIRONMENT_TYPE")
    public EnvironmentTypeOutput delete(String code) {
        EnvironmentType type = finder.findForUpdate(code);
        validator.validateForDelete(type);
        EnvironmentTypeOutput removedType = EnvironmentTypeOutput.from(type);
        types.delete(type);
        types.flush();
        return removedType;
    }
}
