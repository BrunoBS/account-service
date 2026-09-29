package br.com.portalmanager.platform.workspace.core.application.infra;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.shared.tagging.ResourceTagStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ApplicationTagStore implements ResourceTagStore {

    private final JdbcClient jdbc;

    public ApplicationTagStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, TagOriginType> findByOwnerId(Long ownerId) {
        Map<String, TagOriginType> result = new LinkedHashMap<>();
        jdbc.sql("SELECT name, origin_type FROM application_tag WHERE application_id = :id ORDER BY name")
                .param("id", ownerId)
                .query((rs, rowNum) -> Map.entry(rs.getString("name"), TagOriginType.valueOf(rs.getString("origin_type"))))
                .list()
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    @Override
    public void insert(Long ownerId, String name, TagOriginType originType) {
        jdbc.sql("INSERT INTO application_tag (application_id, name, origin_type) VALUES (:id, :name, :origin)")
                .param("id", ownerId).param("name", name).param("origin", originType.name()).update();
    }

    @Override
    public void updateOrigin(Long ownerId, String name, TagOriginType originType) {
        jdbc.sql("UPDATE application_tag SET origin_type = :origin WHERE application_id = :id AND name = :name")
                .param("origin", originType.name()).param("id", ownerId).param("name", name).update();
    }

    @Override
    public void delete(Long ownerId, String name) {
        jdbc.sql("DELETE FROM application_tag WHERE application_id = :id AND name = :name")
                .param("id", ownerId).param("name", name).update();
    }

    @Override
    public void deleteAll(Long ownerId) {
        jdbc.sql("DELETE FROM application_tag WHERE application_id = :id").param("id", ownerId).update();
    }

    @Override
    public List<String> findManual(Long ownerId) {
        return jdbc.sql("SELECT name FROM application_tag WHERE application_id = :id AND origin_type = 'MANUAL' ORDER BY name")
                .param("id", ownerId).query(String.class).list();
    }

    @Override
    public Map<String, List<String>> findManualByIdentifiers(Collection<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) return Map.of();
        return jdbc.sql("SELECT a.identifier, t.name FROM application_tag t JOIN applications a ON a.id = t.application_id WHERE a.identifier IN (:ids) AND t.origin_type = 'MANUAL' ORDER BY a.identifier, t.name")
                .param("ids", identifiers)
                .query((rs, rowNum) -> Map.entry(rs.getString("identifier"), rs.getString("name")))
                .list().stream().collect(java.util.stream.Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(Map.Entry::getValue, java.util.stream.Collectors.toList())));
    }

    @Override
    public List<String> findIdentifiersByTag(String normalizedTag) {
        return jdbc.sql("SELECT a.identifier FROM application_tag t JOIN applications a ON a.id = t.application_id WHERE t.name = :name ORDER BY a.identifier")
                .param("name", normalizedTag).query(String.class).list();
    }
}
