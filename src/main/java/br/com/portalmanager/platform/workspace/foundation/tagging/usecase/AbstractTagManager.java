package br.com.portalmanager.platform.workspace.foundation.tagging.usecase;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.tagging.domain.AbstractTagEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractTagManager<T extends AbstractTagEntity, O> {

    protected final void reconcileTags(
            O owner,
            Long ownerId,
            Collection<String> manualTags,
            Collection<String> systemTags) {

        Objects.requireNonNull(ownerId, "tag owner id must not be null");

        Map<String, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<T> current = findByOwnerId(ownerId);
        List<T> obsolete = new ArrayList<>();

        for (T tag : current) {
            TagOriginType desiredOrigin = desired.remove(tag.getName());
            if (desiredOrigin == null) {
                obsolete.add(tag);
            } else if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
            }
        }

        if (!obsolete.isEmpty()) {
            deleteEntities(obsolete);
        }

        List<T> created = desired.entrySet().stream()
                .map(entry -> newTag(owner, entry.getKey(), entry.getValue()))
                .toList();

        if (!created.isEmpty()) {
            saveEntities(created);
        }
    }

    protected final List<String> findManualTags(Long ownerId) {
        return findByOwnerId(ownerId).stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(AbstractTagEntity::getName)
                .toList();
    }

    protected final String normalizeTag(String tag) {
        return TagNormalizer.normalize(tag);
    }

    protected abstract List<T> findByOwnerId(Long ownerId);

    protected abstract T newTag(O owner, String name, TagOriginType originType);

    protected abstract void saveEntities(Collection<T> tags);

    protected abstract void deleteEntities(Collection<T> tags);

    protected abstract void deleteByOwnerId(Long ownerId);

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
