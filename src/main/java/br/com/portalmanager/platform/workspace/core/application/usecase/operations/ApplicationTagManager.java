package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import br.com.portalmanager.platform.workspace.core.application.infra.ApplicationTagStore;
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
public class ApplicationTagManager {

    private final ApplicationTagStore store;

    public ApplicationTagManager(ApplicationTagStore store) {
        this.store = store;
    }

    @Transactional
    public void reconcile(Application app, String workspaceIdentifier, List<String> tags) {
        Objects.requireNonNull(app.getId(), "application.id must not be null");

        Map<String, TagOriginType> desired = desiredTags(tags, ApplicationSystemTags.resolve(app, workspaceIdentifier));
        Map<String, TagOriginType> current = new LinkedHashMap<>(store.findByOwnerId(app.getId()));

        current.forEach((name, origin) -> {
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                store.delete(app.getId(), name);
            } else if (origin != desiredOrigin) {
                store.updateOrigin(app.getId(), name, desiredOrigin);
            }
        });

        desired.forEach((name, origin) -> store.insert(app.getId(), name, origin));
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
