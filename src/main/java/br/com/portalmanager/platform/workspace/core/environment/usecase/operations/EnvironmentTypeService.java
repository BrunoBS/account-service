package br.com.portalmanager.platform.workspace.core.environment.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EnvironmentTypeService {
    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;
    private final EnvironmentTypeValidator validator;

    public EnvironmentTypeService(EnvironmentTypeRepository types, EnvironmentTypeCompatibilityRepository compatibilities,
                                  EnvironmentRepository environments, EnvironmentTypeValidator validator) {
        this.types = types;
        this.compatibilities = compatibilities;
        this.environments = environments;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public EnvironmentType active(String code) {
        return types.findByCodeAndLifecycle(code, "ACTIVE")
                .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND));
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
        EnvironmentType type = find(identifier);
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
        EnvironmentType type = find(identifier);
        if (environments.existsByEnvironmentTypeId(type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        type.inactivate(LocalDateTime.now());
    }

    @Transactional
    public EnvironmentTypeOutput restore(String identifier) {
        EnvironmentType type = find(identifier);
        type.restore(LocalDateTime.now());
        return EnvironmentTypeOutput.from(type);
    }

    @Transactional
    public void delete(String identifier) {
        EnvironmentType type = find(identifier);
        if (!"INACTIVE".equals(type.getLifecycle()) || environments.existsByEnvironmentTypeId(type.getId())
                || compatibilities.existsByParentTypeIdOrChildTypeId(type.getId(), type.getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        types.delete(type);
    }

    @Transactional(readOnly = true)
    public EnvironmentTypeOutput findOutput(String identifier) { return EnvironmentTypeOutput.from(find(identifier)); }
    @Transactional(readOnly = true)
    public List<EnvironmentTypeOutput> list() { return types.findAllByOrderByDisplayOrderAscIdAsc().stream().map(EnvironmentTypeOutput::from).toList(); }

    @Transactional
    public EnvironmentCompatibilityOutput allow(String parentCode, String childCode) {
        EnvironmentType parent = active(parentCode);
        EnvironmentType child = active(childCode);
        if (parent.getId().equals(child.getId())) throw new ValidationException(EnvironmentMessageKeys.COMPATIBILITY_INVALID);
        var existing = compatibilities.findByParentTypeIdAndChildTypeId(parent.getId(), child.getId());
        if (reaches(child.getId(), parent.getId(), new java.util.HashSet<>()))
            throw new ValidationException(EnvironmentMessageKeys.COMPATIBILITY_INVALID);
        if (existing.isPresent()) {
            existing.get().activate(LocalDateTime.now());
            return EnvironmentCompatibilityOutput.from(existing.get());
        }
        // Refuse cycles in the type graph even when the proposed edge is not yet used by an environment.
        return EnvironmentCompatibilityOutput.from(compatibilities.saveAndFlush(
                new EnvironmentTypeCompatibility(parent, child, LocalDateTime.now())));
    }

    private boolean reaches(Long current, Long target, java.util.Set<Long> visited) {
        if (current.equals(target)) return true;
        if (!visited.add(current)) return false;
        return compatibilities.findByLifecycle("ACTIVE").stream()
                .filter(edge -> edge.getParentType().getId().equals(current))
                .anyMatch(edge -> reaches(edge.getChildType().getId(), target, visited));
    }

    @Transactional
    public void disallow(String identifier) {
        EnvironmentTypeCompatibility edge = compatibilities.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
        if (environments.existsByTypePair(edge.getParentType().getId(), edge.getChildType().getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        edge.inactivate(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<EnvironmentCompatibilityOutput> listCompatibilities() {
        return compatibilities.findByLifecycle("ACTIVE").stream().map(EnvironmentCompatibilityOutput::from).toList();
    }

    private EnvironmentType find(String identifier) {
        return types.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND));
    }

}
