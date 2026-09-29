package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import br.com.portalmanager.platform.workspace.core.application.infra.ApplicationTagStore;
import br.com.portalmanager.platform.workspace.shared.tagging.ResourceTagManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ApplicationTagManager {

    private final ApplicationTagStore store;
    private final ResourceTagManager tagManager;

    public ApplicationTagManager(ApplicationTagStore store) {
        this.store = store;
        this.tagManager = new ResourceTagManager(store);
    }

    public void reconcile(Application app, String workspaceIdentifier, List<String> tags) {
        tagManager.reconcile(app.getId(), tags, ApplicationSystemTags.resolve(app, workspaceIdentifier));
    }

    public List<String> findManual(Application app) {
        return store.findManual(app.getId());
    }

    public Map<String, List<String>> findManualByIds(Collection<String> ids) {
        return store.findManualByIdentifiers(ids);
    }

    public List<String> findIdentifiersByTag(String tag) {
        String normalized = TagNormalizer.normalize(tag);
        return normalized == null ? List.of() : store.findIdentifiersByTag(normalized);
    }

    public void deleteAll(Application app) {
        store.deleteAll(app.getId());
    }
}
