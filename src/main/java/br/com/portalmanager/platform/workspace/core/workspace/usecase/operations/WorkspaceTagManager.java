package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.infra.WorkspaceTagStore;
import br.com.portalmanager.platform.workspace.shared.tagging.ResourceTagManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class WorkspaceTagManager {

    private final WorkspaceTagStore store;
    private final ResourceTagManager tagManager;

    public WorkspaceTagManager(WorkspaceTagStore store) {
        this.store = store;
        this.tagManager = new ResourceTagManager(store);
    }

    public void reconcile(Workspace workspace, List<String> manualTags) {
        tagManager.reconcile(workspace.getId(), manualTags, WorkspaceSystemTags.resolve(workspace));
    }

    public List<String> findManual(Workspace workspace) {
        return store.findManual(workspace.getId());
    }

    public Map<String, List<String>> findManualByIdentifiers(Collection<String> identifiers) {
        return store.findManualByIdentifiers(identifiers);
    }

    public List<String> findIdentifiersByTag(String tag) {
        String normalized = TagNormalizer.normalize(tag);
        return normalized == null ? List.of() : store.findIdentifiersByTag(normalized);
    }

    public void deleteAll(Workspace workspace) {
        store.deleteAll(workspace.getId());
    }
}
