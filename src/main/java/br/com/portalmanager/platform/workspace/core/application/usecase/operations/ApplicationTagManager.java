package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.tagging.model.TagOwnerType;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ApplicationTagManager {
    private static final TagOwnerType OWNER = () -> "APPLICATION";
    private final TagManager tagManager;

    public ApplicationTagManager(TagManager tagManager) { this.tagManager = tagManager; }

    public void reconcile(Application app, String workspaceIdentifier, List<String> tags) {
        tagManager.reconcile(OWNER, app.getIdentifier(), tags, ApplicationSystemTags.resolve(app, workspaceIdentifier));
    }
    public List<String> findManual(Application app) { return tagManager.findManual(OWNER, app.getIdentifier()); }
    public Map<String, List<String>> findManualByIds(Collection<String> ids) {
        return tagManager.findManualByOwners(OWNER, ids);
    }
    public List<String> findIdentifiersByTag(String tag) { return tagManager.findOwnerIdsByTag(OWNER, tag); }
    public void deleteAll(Application app) { tagManager.deleteAll(OWNER, app.getIdentifier()); }
}
