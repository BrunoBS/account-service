package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.infra.WorkspaceTagStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class WorkspaceTagManager {

    private final WorkspaceTagStore store;

    public WorkspaceTagManager(WorkspaceTagStore store) {
        this.store = store;
    }

    @Transactional
    public void reconcile(Workspace workspace, List<String> manualTags) {
        Objects.requireNonNull(workspace.getId(), "workspace.id must not be null");

        Map<String, TagOriginType> desired = desiredTags(manualTags, WorkspaceSystemTags.resolve(workspace));
        Map<String, TagOriginType> current = new LinkedHashMap<>(store.findByOwnerId(workspace.getId()));

        current.forEach((name, origin) -> {
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                store.delete(workspace.getId(), name);
            } else if (origin != desiredOrigin) {
                store.updateOrigin(workspace.getId(), name, desiredOrigin);
            }
        });

        desired.forEach((name, origin) -> store.insert(workspace.getId(), name, origin));
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

    private static Map<String, TagOriginType> desiredTags(Collection<String> manualTags, Collection<String> systemTags) {
        Map<String, TagOriginType> desired = new LinkedHashMap<>();
        normalize(manualTags).forEach(name -> desired.put(name, TagOriginType.MANUAL));
        normalize(systemTags).forEach(name -> desired.putIfAbsent(name, TagOriginType.SYSTEM));
        return desired;
    }

    private static Set<String> normalize(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        values.stream()
                .map(TagNormalizer::normalize)
                .filter(Objects::nonNull)
                .forEach(normalized::add);
        return normalized;
    }
}
