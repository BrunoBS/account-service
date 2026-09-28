package br.com.portalmanager.platform.workspace.core.environment.usecase.operations;

import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentValidator;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
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

    public EnvironmentCommandService(EnvironmentRepository repository, EnvironmentFinder finder, EnvironmentQueryService visibility, WorkspaceQueryService workspaceVisibility, WorkspaceReferenceResolver workspaces, EnvironmentNormalizer normalizer, EnvironmentValidator validator) {
        this.repository = repository;
        this.finder = finder;
        this.visibility = visibility;
        this.workspaceVisibility = workspaceVisibility;
        this.workspaces = workspaces;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    @Transactional
    public EnvironmentOutput createCustom(String workspaceIdentifier, CreateEnvironmentInput input) {
        workspaceVisibility.findByIdentifier(workspaceIdentifier);
        return create(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, input);
    }

    @Transactional
    public EnvironmentOutput createDefault(CreateEnvironmentInput input) {
        return create(null, null, input);
    }

    private EnvironmentOutput create(Long workspaceId, String workspaceIdentifier, CreateEnvironmentInput raw) {
        CreateEnvironmentInput input = normalizer.normalize(raw);
        validator.validateForCreate(input, input != null && input.name() != null && existsName(workspaceId, input.name(), null));
        Integer lastOrder = workspaceId == null ? repository.maxDefaultSortOrder() : repository.maxCustomSortOrder(workspaceId);
        int sortOrder = input.sortOrder() != null ? input.sortOrder() : workspaceId == null ? (lastOrder == null ? 1 : lastOrder + 1) : (lastOrder == null ? 4 : lastOrder + 1);
        Environment environment = new Environment(workspaceId, input.name(), input.description(), AuthorizationTypeCode.of(input.authorizationType()), input.authorizerGroup(), input.settings(), sortOrder, LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

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
        validator.validateForUpdate(input, input != null && input.name() != null && existsName(workspaceId, input.name(), environment.getId()));
        validator.requireVersion(environment.getVersion(), input.version());
        environment.update(input.name(), input.description(), AuthorizationTypeCode.of(input.authorizationType()), input.authorizerGroup(), input.settings(), input.sortOrder() == null ? environment.getSortOrder() : input.sortOrder(), LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Transactional
    public void inactivateCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustom(workspaceIdentifier, identifier);
        inactivate(workspaces.resolveInternalId(workspaceIdentifier), identifier);
    }

    @Transactional
    public void inactivateDefault(String identifier) {
        visibility.findDefault(identifier);
        inactivate(null, identifier);
    }

    private void inactivate(Long workspaceId, String identifier) {
        Environment environment = finder.findActive(identifier, workspaceId);
        environment.inactivate(LocalDateTime.now());
        repository.saveAndFlush(environment);
    }

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
        validator.validateForCreate(new CreateEnvironmentInput(environment.getName(), environment.getDescription(), environment.getAuthorizationType().value(), environment.getSortOrder(), environment.getAuthorizerGroup(), environment.getSettings()), existsName(workspaceId, environment.getName(), environment.getId()));
        environment.restore(LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Transactional
    public void deleteCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustomForDeletion(workspaceIdentifier, identifier);
        delete(workspaces.resolveInternalId(workspaceIdentifier), identifier);
    }

    @Transactional
    public void deleteDefault(String identifier) {
        visibility.findDefaultForDeletion(identifier);
        delete(null, identifier);
    }

    private void delete(Long workspaceId, String identifier) {
        Environment environment = finder.findInactiveForDeletion(identifier, workspaceId);
        environment.quarantine(LocalDateTime.now());
        repository.saveAndFlush(environment);
    }

    private boolean existsName(Long workspaceId, String name, Long excludedId) {
        if (workspaceId == null)
            return excludedId == null ? repository.existsByNameAndWorkspaceIdIsNull(name) : repository.existsByNameAndWorkspaceIdIsNullAndIdNot(name, excludedId);
        return excludedId == null ? repository.existsByNameAndWorkspaceId(name, workspaceId) : repository.existsByNameAndWorkspaceIdAndIdNot(name, workspaceId, excludedId);
    }
}
