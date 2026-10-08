package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;

import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentValidator;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTopologyValidator;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class EnvironmentCommandService {
    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;
    private final EnvironmentQueryService visibility;
    private final WorkspaceQueryService workspaceVisibility;
    private final WorkspaceReferenceResolver workspaces;
    private final EnvironmentNormalizer normalizer;
    private final EnvironmentValidator validator;
    private final EnvironmentTopologyValidator topology;
    private final EnvironmentTypeQueryService types;

    public EnvironmentCommandService(EnvironmentRepository repository, EnvironmentFinder finder, EnvironmentQueryService visibility,
                                     WorkspaceQueryService workspaceVisibility, WorkspaceReferenceResolver workspaces,
                                     EnvironmentNormalizer normalizer, EnvironmentValidator validator,
                                     EnvironmentTopologyValidator topology, EnvironmentTypeQueryService types) {
        this.repository = repository;
        this.finder = finder;
        this.visibility = visibility;
        this.workspaceVisibility = workspaceVisibility;
        this.workspaces = workspaces;
        this.normalizer = normalizer;
        this.validator = validator;
        this.topology = topology;
        this.types = types;
    }

    @Auditable(action = AuditAction.CREATE, event = "ENVIRONMENT_CREATED", resourceType = "ENVIRONMENT")
    @Transactional
    public EnvironmentOutput createCustom(String workspaceIdentifier, CreateEnvironmentInput input) {
        workspaceVisibility.findByIdentifier(workspaceIdentifier);
        return create(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, input);
    }

    @Transactional
    public EnvironmentOutput createDefault(CreateEnvironmentInput input) { return create(null, null, input); }

    private EnvironmentOutput create(Long workspaceId, String workspaceIdentifier, CreateEnvironmentInput raw) {
        CreateEnvironmentInput input = normalizer.normalize(raw);
        if (input == null) validator.validateForCreate(null, false);
        String typeCode = input.environmentTypeCode() == null
                ? workspaceId == null ? "DEFAULT" : "CUSTOM" : input.environmentTypeCode();
        EnvironmentType type = types.active(typeCode);
        Environment parent = resolveParent(input.parentIdentifier(), workspaceId);
        Long parentId = parent == null ? null : parent.getId();
        boolean duplicateName = input != null && existsName(workspaceId, parentId, input.name(), null);
        validator.validateForCreate(input, duplicateName);
        topology.validate(type, workspaceId, parent);

        int sortOrder = sortOrderForCreate(workspaceId, parentId, input.sortOrder());
        AuthorizationTypeCode authorizationType = AuthorizationTypeCode.of(input.authorizationType());
        Environment environment = new Environment(workspaceId, type, parent, input.name(), input.description(),
                authorizationType, input.authorizerGroup(), input.settings(), sortOrder, LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Auditable(action = AuditAction.UPDATE, event = "ENVIRONMENT_UPDATED", resourceType = "ENVIRONMENT")
    @Transactional
    public EnvironmentOutput updateCustom(String workspaceIdentifier, String identifier, UpdateEnvironmentInput raw) {
        visibility.findCustom(workspaceIdentifier, identifier);
        return update(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier, raw);
    }

    @Transactional
    public EnvironmentOutput updateDefault(String identifier, UpdateEnvironmentInput raw) {
        visibility.findDefault(identifier);
        return update(null, null, identifier, raw);
    }

    private EnvironmentOutput update(Long workspaceId, String workspaceIdentifier, String identifier, UpdateEnvironmentInput raw) {
        Environment environment = finder.findActive(identifier, workspaceId);
        UpdateEnvironmentInput input = normalizer.normalize(raw);
        boolean duplicateName = input != null && existsName(workspaceId, parentId(environment), input.name(), environment.getId());
        validator.validateForUpdate(input, duplicateName);
        validator.requireVersion(environment.getVersion(), input.version());

        EnvironmentType type = input.environmentTypeCode() == null
                ? environment.getEnvironmentType() : types.active(input.environmentTypeCode());
        topology.validate(type, workspaceId, environment.getParent());
        boolean changingTypeWithChildren = !type.getId().equals(environment.getEnvironmentType().getId())
                && repository.existsByParentId(environment.getId());
        topology.requireTypeChangeWithoutChildren(changingTypeWithChildren);
        int sortOrder = input.sortOrder() == null ? environment.getSortOrder() : input.sortOrder();
        AuthorizationTypeCode authorizationType = AuthorizationTypeCode.of(input.authorizationType());
        environment.update(input.name(), input.description(), authorizationType,
                input.authorizerGroup(), input.settings(), sortOrder, LocalDateTime.now());
        environment.changeType(type, LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Auditable(action = AuditAction.DEACTIVATE, event = "ENVIRONMENT_DEACTIVATED", resourceType = "ENVIRONMENT")
    @Transactional
    public EnvironmentOutput inactivateCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustom(workspaceIdentifier, identifier);
        return inactivate(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }

    @Transactional
    public void inactivateDefault(String identifier) { visibility.findDefault(identifier); inactivate(null, null, identifier); }
    private EnvironmentOutput inactivate(Long workspaceId, String workspaceIdentifier, String identifier) {
        Environment environment = finder.findActive(identifier, workspaceId);
        environment.inactivate(LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Auditable(action = AuditAction.RESTORE, event = "ENVIRONMENT_RESTORED", resourceType = "ENVIRONMENT")
    @Transactional
    public EnvironmentOutput restoreCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustomInactive(workspaceIdentifier, identifier);
        return restore(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }

    @Transactional
    public EnvironmentOutput restoreDefault(String identifier) {
        visibility.findDefaultInactive(identifier);
        return restore(null, null, identifier);
    }

    private EnvironmentOutput restore(Long workspaceId, String workspaceIdentifier, String identifier) {
        Environment environment = finder.findInactive(identifier, workspaceId);
        CreateEnvironmentInput input = new CreateEnvironmentInput(environment.getName(), environment.getDescription(),
                environment.getAuthorizationType().value(), environment.getSortOrder(), environment.getAuthorizerGroup(),
                environment.getSettings(), environment.getEnvironmentType().getCode(),
                environment.getParent() == null ? null : environment.getParent().getIdentifier());
        boolean duplicateName = existsName(workspaceId, parentId(environment), environment.getName(), environment.getId());
        validator.validateForCreate(input, duplicateName);
        topology.requireParentAccessible(environment.getParent() == null || finder.accessible(environment.getParent()));
        topology.validate(types.active(environment.getEnvironmentType().getCode()), workspaceId, environment.getParent());
        environment.restore(LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Auditable(action = AuditAction.DELETE, event = "ENVIRONMENT_DELETED", resourceType = "ENVIRONMENT")
    @Transactional
    public EnvironmentOutput deleteCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustomForDeletion(workspaceIdentifier, identifier);
        return delete(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }

    @Transactional
    public void deleteDefault(String identifier) { visibility.findDefaultForDeletion(identifier); delete(null, null, identifier); }
    private EnvironmentOutput delete(Long workspaceId, String workspaceIdentifier, String identifier) {
        Environment environment = finder.findInactiveForDeletion(identifier, workspaceId);
        environment.quarantine(LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    private Environment resolveParent(String identifier, Long workspaceId) {
        if (identifier == null) return null;
        Environment parent = repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        if ((workspaceId == null && parent.getWorkspaceId() != null)
                || (parent.getWorkspaceId() != null && !parent.getWorkspaceId().equals(workspaceId))
                || !finder.accessible(parent))
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        return parent;
    }

    private Long parentId(Environment environment) {
        return environment.getParent() == null ? null : environment.getParent().getId();
    }

    private int sortOrderForCreate(Long workspaceId, Long parentId, Integer requestedOrder) {
        if (requestedOrder != null) return requestedOrder;

        Integer lastOrder = repository.maxSiblingSortOrder(workspaceId, parentId);
        if (lastOrder != null) return lastOrder + 1;

        return workspaceId == null ? 1 : 4;
    }

    private boolean existsName(Long workspaceId, Long parentId, String name, Long excludedId) {
        if (name == null) return false;
        return repository.existsSibling(name, workspaceId, parentId, excludedId);
    }
}
