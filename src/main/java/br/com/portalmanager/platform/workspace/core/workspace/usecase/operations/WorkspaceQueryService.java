package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceQueryService {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace> tags;

    public WorkspaceQueryService(
        WorkspaceRepository repository,
        WorkspaceFinder finder,
        WorkspaceNormalizer normalizer,
        WorkspaceValidator validator,
        TagManager<WorkspaceTag, Workspace> tags
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return finder.findActive(identifier).getId();
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long id) {
        return finder.findActive(id).getIdentifier();
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public WorkspaceOutput findByIdentifier(String identifier) {
        Workspace workspace = finder.findActive(identifier);
        return WorkspaceOutput.from(workspace, tags.findManual(workspace));
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public List<WorkspaceOutput> findAll(FindAllWorkspacesInput input) {
        String lifecycleCode =
            input != null && Boolean.FALSE.equals(input.active())
                ? LifecycleTypeCode.inactive().value()
                : LifecycleTypeCode.active().value();
        String normalizedType = normalizer.normalizeTypeFilter(input == null ? null : input.typeName());
        String normalizedTag = normalizer.normalizeTagFilter(input == null ? null : input.tagName());
        validator.validateTypeFilter(normalizedType);
        List<Workspace> workspaces;
        if (normalizedTag == null) workspaces = repository.findFiltered(lifecycleCode, normalizedType);
        else {
            List<String> identifiers = tags.findOwnerKeysByTag(normalizedTag);
            if (identifiers.isEmpty()) return List.of();
            workspaces = repository.findFilteredByIdentifiers(lifecycleCode, normalizedType, identifiers);
        }
        Map<String, List<String>> manualTags = tags.findManualByOwnerKeys(
            workspaces.stream().map(Workspace::getIdentifier).toList()
        );
        return workspaces
            .stream()
            .map(w -> WorkspaceOutput.from(w, manualTags.getOrDefault(w.getIdentifier(), List.of())))
            .toList();
    }
}
