package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceTagRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class WorkspaceQueryService {
    private final WorkspaceRepository repository;
    private final WorkspaceTagRepository tagRepository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace, Long, String> tags;

    public WorkspaceQueryService(WorkspaceRepository repository, WorkspaceTagRepository tagRepository,
                                 WorkspaceFinder finder, WorkspaceNormalizer normalizer,
                                 WorkspaceValidator validator,
                                 TagManager<WorkspaceTag, Workspace, Long, String> tags) {
        this.repository = repository;
        this.tagRepository = tagRepository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireActive(workspace);
        return workspace.getId();
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long id) {
        Workspace workspace = finder.findById(id);
        validator.requireActive(workspace);
        return workspace.getIdentifier();
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public WorkspaceOutput findByIdentifier(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireActive(workspace);
        return WorkspaceOutput.from(workspace, tags.findManual(workspace));
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public List<WorkspaceOutput> findAll(FindAllWorkspacesInput input) {
        LifecycleTypeCode lifecycle = input == null || input.lifecycle() == null
                ? LifecycleTypeCode.active()
                : input.lifecycle();
        String lifecycleCode = lifecycle.value();

        String typeFilter = normalizer.normalizeTypeFilter(input == null ? null : input.typeName());
        String tagFilter = normalizer.normalizeTagFilter(input == null ? null : input.tagName());
        validator.validateTypeFilter(typeFilter);

        List<Workspace> workspaces;
        if (tagFilter == null) {
            workspaces = repository.findFiltered(lifecycleCode, typeFilter);
        } else {
            List<String> identifiers = tagRepository.findWorkspaceIdentifiersByTagStartingWith(tagFilter);
            if (identifiers.isEmpty()) {
                return List.of();
            }
            workspaces = repository.findFilteredByIdentifiers(lifecycleCode, typeFilter, identifiers);
        }

        List<String> workspaceIdentifiers = workspaces.stream()
                .map(Workspace::getIdentifier)
                .toList();
        Map<String, List<String>> manualTags = tags.findManualByOwnerKeys(workspaceIdentifiers);

        return workspaces.stream()
                .map(workspace -> WorkspaceOutput.from(
                        workspace,
                        manualTags.getOrDefault(workspace.getIdentifier(), List.of())
                ))
                .toList();
    }
}
