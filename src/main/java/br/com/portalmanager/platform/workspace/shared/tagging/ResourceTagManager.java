package br.com.portalmanager.platform.workspace.shared.tagging;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResourceTagManager {

    private final ResourceTagStore store;

    public ResourceTagManager(ResourceTagStore store) {
        this.store = store;
    }

    @Transactional
    public void reconcile(Long ownerId, Collection<String> manualTags, Collection<String> systemTags) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Map<String, TagOriginType> desired = desiredTags(manualTags, systemTags);
        Map<String, TagOriginType> current = new LinkedHashMap<>(store.findByOwnerId(ownerId));

        current.forEach((name, origin) -> {
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                store.delete(ownerId, name);
            } else if (origin != desiredOrigin) {
                store.updateOrigin(ownerId, name, desiredOrigin);
            }
        });

        desired.forEach((name, origin) -> store.insert(ownerId, name, origin));
    }

    private static Map<String, TagOriginType> desiredTags(
            Collection<String> manualTags,
            Collection<String> systemTags) {
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
