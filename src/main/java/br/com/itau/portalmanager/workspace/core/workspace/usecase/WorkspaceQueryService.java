package br.com.itau.portalmanager.workspace.core.workspace.usecase;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceFinder;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceNormalizer;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.authorization.annotation.ResourceVisibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class WorkspaceQueryService {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final WorkspaceTaggingSupport taggingSupport;

    public WorkspaceQueryService(
            WorkspaceRepository repository,
            WorkspaceFinder finder,
            WorkspaceNormalizer normalizer,
            WorkspaceValidator validator,
            WorkspaceTaggingSupport taggingSupport
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.taggingSupport = taggingSupport;
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public WorkspaceOutput findById(Long id) {
        Workspace workspace = finder.findActive(id);
        return WorkspaceOutput.from(workspace, taggingSupport.findManual(workspace));
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public List<WorkspaceOutput> findAll(FindAllWorkspacesInput input) {
        LifecycleTypeEnum lifecycle = input != null && Boolean.FALSE.equals(input.active())
                ? LifecycleTypeEnum.INACTIVE
                : LifecycleTypeEnum.ACTIVE;

        String normalizedType = normalizer.normalizeTypeFilter(input == null ? null : input.typeName());
        String normalizedTag = normalizer.normalizeTagFilter(input == null ? null : input.tagName());
        validator.validateTypeFilter(normalizedType);

        WorkspaceTypeEnum workspaceType = normalizedType == null
                ? null
                : WorkspaceTypeEnum.valueOf(normalizedType);

        List<Workspace> workspaces;
        if (normalizedTag == null) {
            workspaces = repository.findFiltered(lifecycle, workspaceType);
        } else {
            List<String> identifiers = taggingSupport.findIdentifiersByTag(normalizedTag);
            if (identifiers.isEmpty()) {
                return List.of();
            }
            workspaces = repository.findFilteredByIdentifiers(lifecycle, workspaceType, identifiers);
        }

        Map<String, List<String>> manualTags = taggingSupport.findManualByIdentifiers(
                workspaces.stream().map(Workspace::getIdentifier).toList()
        );

        return workspaces.stream()
                .map(workspace -> WorkspaceOutput.from(
                        workspace,
                        manualTags.getOrDefault(workspace.getIdentifier(), List.of())
                ))
                .toList();
    }
}
