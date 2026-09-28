package br.com.portalmanager.platform.workspace.core.environment.usecase.operations;

import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentValidator;
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

    public EnvironmentCommandService(EnvironmentRepository repository, EnvironmentFinder finder, EnvironmentQueryService visibility,
                                     WorkspaceQueryService workspaceVisibility, WorkspaceReferenceResolver workspaces,
                                     EnvironmentNormalizer normalizer, EnvironmentValidator validator) {
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
    public EnvironmentOutput createDefault(CreateEnvironmentInput input) { return create(null, null, input); }

    private EnvironmentOutput create(Long workspaceId, String workspaceIdentifier, CreateEnvironmentInput raw) {
        CreateEnvironmentInput input = normalizer.normalize(raw);
        boolean duplicateName = input != null && existsName(workspaceId, input.name(), null);
        validator.validateForCreate(input, duplicateName);

        int sortOrder = sortOrderForCreate(workspaceId, input.sortOrder());
        AuthorizationTypeCode authorizationType = AuthorizationTypeCode.of(input.authorizationType());
        Environment environment = new Environment(workspaceId, input.name(), input.description(),
                authorizationType, input.authorizerGroup(), input.settings(), sortOrder, LocalDateTime.now());
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
        boolean duplicateName = input != null && existsName(workspaceId, input.name(), environment.getId());
        validator.validateForUpdate(input, duplicateName);
        validator.requireVersion(environment.getVersion(), input.version());

        int sortOrder = input.sortOrder() == null ? environment.getSortOrder() : input.sortOrder();
        AuthorizationTypeCode authorizationType = AuthorizationTypeCode.of(input.authorizationType());
        environment.update(input.name(), input.description(), authorizationType,
                input.authorizerGroup(), input.settings(), sortOrder, LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Transactional
    public void inactivateCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustom(workspaceIdentifier, identifier);
        inactivate(workspaces.resolveInternalId(workspaceIdentifier), identifier);
    }

    @Transactional
    public void inactivateDefault(String identifier) { visibility.findDefault(identifier); inactivate(null, identifier); }
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
        CreateEnvironmentInput input = new CreateEnvironmentInput(environment.getName(), environment.getDescription(),
                environment.getAuthorizationType().value(), environment.getSortOrder(), environment.getAuthorizerGroup(),
                environment.getSettings());
        boolean duplicateName = existsName(workspaceId, environment.getName(), environment.getId());
        validator.validateForCreate(input, duplicateName);
        environment.restore(LocalDateTime.now());
        return EnvironmentOutput.from(repository.saveAndFlush(environment), workspaceIdentifier);
    }

    @Transactional
    public void deleteCustom(String workspaceIdentifier, String identifier) {
        visibility.findCustomForDeletion(workspaceIdentifier, identifier);
        delete(workspaces.resolveInternalId(workspaceIdentifier), identifier);
    }

    @Transactional
    public void deleteDefault(String identifier) { visibility.findDefaultForDeletion(identifier); delete(null, identifier); }
    private void delete(Long workspaceId, String identifier) {
        Environment environment = finder.findInactiveForDeletion(identifier, workspaceId);
        environment.quarantine(LocalDateTime.now());
        repository.saveAndFlush(environment);
    }

    private int sortOrderForCreate(Long workspaceId, Integer requestedOrder) {
        if (requestedOrder != null) return requestedOrder;

        Integer lastOrder = workspaceId == null
                ? repository.maxDefaultSortOrder()
                : repository.maxCustomSortOrder(workspaceId);
        if (lastOrder != null) return lastOrder + 1;

        return workspaceId == null ? 1 : 4;
    }

    private boolean existsName(Long workspaceId, String name, Long excludedId) {
        if (name == null) return false;

        if (workspaceId == null) {
            if (excludedId == null) return repository.existsByNameAndWorkspaceIdIsNull(name);
            return repository.existsByNameAndWorkspaceIdIsNullAndIdNot(name, excludedId);
        }

        if (excludedId == null) return repository.existsByNameAndWorkspaceId(name, workspaceId);
        return repository.existsByNameAndWorkspaceIdAndIdNot(name, workspaceId, excludedId);
    }
}
