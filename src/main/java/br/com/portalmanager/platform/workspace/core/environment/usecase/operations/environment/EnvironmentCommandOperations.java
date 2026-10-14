package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.EnvironmentMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeFinder;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTopologyValidator;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentValidator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnvironmentCommandOperations {

    private static final int DEFAULT_FIRST_SORT_ORDER = 1;
    private static final int WORKSPACE_FIRST_SORT_ORDER = 4;

    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;
    private final EnvironmentNormalizer normalizer;
    private final EnvironmentValidator validator;
    private final EnvironmentTopologyValidator topology;
    private final EnvironmentTypeFinder typeFinder;
    private final EnvironmentMapper mapper;

    public EnvironmentCommandOperations(
        EnvironmentRepository repository,
        EnvironmentFinder finder,
        EnvironmentNormalizer normalizer,
        EnvironmentValidator validator,
        EnvironmentTopologyValidator topology,
        EnvironmentTypeFinder typeFinder,
        EnvironmentMapper mapper
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.topology = topology;
        this.typeFinder = typeFinder;
        this.mapper = mapper;
    }

    public EnvironmentOutput create(Long workspaceId, String workspaceIdentifier, CreateEnvironmentInput rawInput) {
        CreateEnvironmentInput input = normalizer.normalize(rawInput);
        validator.validateForCreate(input);

        String expectedParentTypeCode = findExpectedParentTypeCode(input.parentIdentifier());
        EnvironmentType environmentType = typeFinder.findForEnvironmentCreation(
            input.environmentTypeCode(), workspaceId != null, expectedParentTypeCode
        );
        Environment parentEnvironment = findParentForCreate(input.parentIdentifier(), workspaceId);
        topology.validateParentTypeUnchanged(expectedParentTypeCode, environmentTypeCode(parentEnvironment));

        Long parentId = parentEnvironment == null ? null : parentEnvironment.getId();
        validator.validateNameAvailable(input.name(), workspaceId, parentId, null);
        topology.validateForNewLink(environmentType, workspaceId, parentEnvironment);

        int sortOrder = sortOrderForCreate(workspaceId, parentId, input.sortOrder());
        LocalDateTime now = LocalDateTime.now();
        Environment environment = new Environment(
            workspaceId,
            environmentType,
            parentEnvironment,
            mapper.toDetails(input, sortOrder),
            now
        );
        return saveAndReturnOutput(environment, workspaceIdentifier);
    }

    public EnvironmentOutput update(Long workspaceId, String workspaceIdentifier, String identifier, UpdateEnvironmentInput rawInput) {
        UpdateEnvironmentInput input = normalizer.normalize(rawInput);
        validator.validateForUpdate(input);

        // Lock types before environment rows, then revalidate the parent's type under its row lock.
        Environment environmentSnapshot = finder.findActive(identifier, workspaceId);
        String expectedParentTypeCode = environmentTypeCode(environmentSnapshot.getParent());
        EnvironmentType requestedType = typeFinder.findForEnvironmentUpdate(
            environmentSnapshot.getEnvironmentType().getCode(), input.environmentTypeCode(), expectedParentTypeCode
        );
        Environment environment = findActiveForUpdate(identifier, workspaceId);

        validator.requireVersion(environment.getVersion(), input.version());
        topology.validateParentTypeUnchanged(expectedParentTypeCode, finder.findParentTypeCodeForValidation(environment));
        validator.validateNameAvailable(input.name(), workspaceId, parentIdOf(environment), environment.getId());
        topology.validateForUpdate(environment, requestedType);

        int sortOrder = input.sortOrder() == null ? environment.getSortOrder() : input.sortOrder();
        LocalDateTime now = LocalDateTime.now();
        environment.update(mapper.toDetails(input, sortOrder), now);
        environment.changeType(requestedType, now);
        return saveAndReturnOutput(environment, workspaceIdentifier);
    }

    public EnvironmentOutput inactivate(Long workspaceId, String workspaceIdentifier, String identifier) {
        Environment environment = findActiveForUpdate(identifier, workspaceId);
        environment.inactivate(LocalDateTime.now());
        return saveAndReturnOutput(environment, workspaceIdentifier);
    }

    public EnvironmentOutput restore(Long workspaceId, String workspaceIdentifier, String identifier) {
        // Preserve the same type-before-environment lock order used by create and update.
        Environment environmentSnapshot = finder.findInactive(identifier, workspaceId);
        Long expectedVersion = environmentSnapshot.getVersion();
        String expectedParentTypeCode = environmentTypeCode(environmentSnapshot.getParent());
        EnvironmentType environmentType = typeFinder.findForEnvironmentRestoration(
            environmentSnapshot.getEnvironmentType().getCode(), expectedParentTypeCode
        );
        Environment environment = findInactiveForRestore(identifier, workspaceId);

        validator.requireVersion(environment.getVersion(), expectedVersion);
        topology.validateParentTypeUnchanged(expectedParentTypeCode, finder.findParentTypeCodeForValidation(environment));
        topology.validateForExistingLink(environmentType, workspaceId, environment.getParent());
        environment.restore(LocalDateTime.now());
        return saveAndReturnOutput(environment, workspaceIdentifier);
    }

    public EnvironmentOutput delete(Long workspaceId, String workspaceIdentifier, String identifier) {
        Environment environment = findInactiveForDeletionForUpdate(identifier, workspaceId);
        validator.validateForDelete(environment);
        environment.quarantine(LocalDateTime.now());
        return saveAndReturnOutput(environment, workspaceIdentifier);
    }

    private Environment findActiveForUpdate(String identifier, Long workspaceId) {
        if (workspaceId == null) return finder.findActiveForUpdate(identifier, null);
        return finder.findWorkspaceActiveForUpdate(identifier, workspaceId);
    }

    private Environment findInactiveForRestore(String identifier, Long workspaceId) {
        if (workspaceId == null) return finder.findInactiveForRestore(identifier, null);
        return finder.findWorkspaceInactiveForRestore(identifier, workspaceId);
    }

    private Environment findInactiveForDeletionForUpdate(String identifier, Long workspaceId) {
        if (workspaceId == null) return finder.findInactiveForDeletionForUpdate(identifier, null);
        return finder.findWorkspaceInactiveForDeletionForUpdate(identifier, workspaceId);
    }

    private String findExpectedParentTypeCode(String parentIdentifier) {
        if (parentIdentifier == null) return null;
        // Types must be locked first; the parent row and its type are revalidated afterwards.
        return repository.findTypeCodeByIdentifier(parentIdentifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
    }

    private Environment findParentForCreate(String parentIdentifier, Long workspaceId) {
        if (parentIdentifier == null) return null;
        Environment parentSnapshot = repository.findByIdentifier(parentIdentifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        Long parentWorkspaceId = parentSnapshot.getWorkspaceId();
        if (parentWorkspaceId != null && !parentWorkspaceId.equals(workspaceId)) {
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        }
        return findActiveForUpdate(parentIdentifier, parentWorkspaceId);
    }

    private EnvironmentOutput saveAndReturnOutput(Environment environment, String workspaceIdentifier) {
        Environment savedEnvironment = repository.saveAndFlush(environment);
        return EnvironmentOutput.from(savedEnvironment, workspaceIdentifier);
    }

    private String environmentTypeCode(Environment environment) {
        return environment == null ? null : environment.getEnvironmentType().getCode();
    }

    private Long parentIdOf(Environment environment) {
        return environment.getParent() == null ? null : environment.getParent().getId();
    }

    private int sortOrderForCreate(Long workspaceId, Long parentId, Integer requestedOrder) {
        if (requestedOrder != null) return requestedOrder;

        Integer highestSiblingSortOrder = repository.maxSiblingSortOrder(workspaceId, parentId);
        if (highestSiblingSortOrder == null) {
            return workspaceId == null ? DEFAULT_FIRST_SORT_ORDER : WORKSPACE_FIRST_SORT_ORDER;
        }
        if (highestSiblingSortOrder == Integer.MAX_VALUE) {
            throw new ValidationException(EnvironmentMessageKeys.SORT_INVALID);
        }
        return highestSiblingSortOrder + 1;
    }
}
