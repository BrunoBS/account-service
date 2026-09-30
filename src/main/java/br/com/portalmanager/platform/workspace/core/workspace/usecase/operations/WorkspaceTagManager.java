package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceTagRepository;
import br.com.portalmanager.platform.workspace.foundation.tagging.usecase.AbstractTagManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkspaceTagManager extends AbstractTagManager<WorkspaceTag, Workspace> {

    private final WorkspaceTagRepository repository;

    public WorkspaceTagManager(WorkspaceTagRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void reconcile(Workspace workspace, List<String> manualTags) {
        reconcileTags(
                workspace,
                workspace.getId(),
                manualTags,
                WorkspaceSystemTags.resolve(workspace)
        );
    }

    @Transactional(readOnly = true)
    public List<String> findManual(Workspace workspace) {
        return findManualTags(workspace.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> findManualByIdentifiers(Collection<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Map.of();
        }

        return repository.findByWorkspaceIdentifiersAndOrigin(identifiers, TagOriginType.MANUAL)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getWorkspace().getIdentifier(),
                        LinkedHashMap::new,
                        Collectors.mapping(WorkspaceTag::getName, Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public List<String> findIdentifiersByTag(String tag) {
        String normalized = normalizeTag(tag);
        return normalized == null
                ? List.of()
                : repository.findWorkspaceIdentifiersByTag(normalized);
    }

    @Transactional
    public void deleteAll(Workspace workspace) {
        deleteByOwnerId(workspace.getId());
    }

    @Override
    protected List<WorkspaceTag> findByOwnerId(Long ownerId) {
        return repository.findByWorkspaceId(ownerId);
    }

    @Override
    protected WorkspaceTag newTag(Workspace owner, String name, TagOriginType originType) {
        return new WorkspaceTag(owner, name, originType);
    }

    @Override
    protected void saveEntities(Collection<WorkspaceTag> tags) {
        repository.saveAll(tags);
    }

    @Override
    protected void deleteEntities(Collection<WorkspaceTag> tags) {
        repository.deleteAll(tags);
    }

    @Override
    protected void deleteByOwnerId(Long ownerId) {
        repository.deleteByWorkspaceId(ownerId);
    }
}
