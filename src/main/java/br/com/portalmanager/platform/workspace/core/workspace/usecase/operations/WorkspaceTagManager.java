package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.tagging.model.TagOwnerType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class WorkspaceTagManager {

    private static final TagOwnerType WORKSPACE_OWNER = () -> "WORKSPACE";

    private final TagManager tagManager;

    public WorkspaceTagManager(TagManager tagManager) {
        this.tagManager = tagManager;
    }

    public void reconcile(Workspace workspace, List<String> manualTags) {
        tagManager.reconcile(
                WORKSPACE_OWNER,
                workspace.getIdentifier(),
                manualTags,
                WorkspaceSystemTags.resolve(workspace)
        );
    }

    public List<String> findManual(Workspace workspace) {
        return tagManager.findManual(WORKSPACE_OWNER, workspace.getIdentifier());
    }

    public Map<String, List<String>> findManualByIdentifiers(Collection<String> identifiers) {
        return tagManager.findManualByOwners(WORKSPACE_OWNER, identifiers);
    }

    public List<String> findIdentifiersByTag(String normalizedTag) {
        return tagManager.findOwnerIdsByTag(WORKSPACE_OWNER, normalizedTag);
    }

    public void deleteAll(Workspace workspace) {
        tagManager.deleteAll(WORKSPACE_OWNER, workspace.getIdentifier());
    }
}
