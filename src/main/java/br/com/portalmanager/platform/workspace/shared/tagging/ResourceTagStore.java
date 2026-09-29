package br.com.portalmanager.platform.workspace.shared.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ResourceTagStore {

    Map<String, TagOriginType> findByOwnerId(Long ownerId);

    void insert(Long ownerId, String name, TagOriginType originType);

    void updateOrigin(Long ownerId, String name, TagOriginType originType);

    void delete(Long ownerId, String name);

    void deleteAll(Long ownerId);

    List<String> findManual(Long ownerId);

    Map<String, List<String>> findManualByIdentifiers(Collection<String> identifiers);

    List<String> findIdentifiersByTag(String normalizedTag);
}
