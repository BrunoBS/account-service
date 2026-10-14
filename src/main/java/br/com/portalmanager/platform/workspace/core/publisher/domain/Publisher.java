package br.com.portalmanager.platform.workspace.core.publisher.domain;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "publishers")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String identifier;

    @Column(nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "publisher_scope", nullable = false, length = 50))
    private ResourceScopeTypeCode scope;

    @Column(nullable = false)
    private boolean deprecated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "json")
    private JsonNode settings;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Publisher() {}

    public Publisher(String code, PublisherDetails details, LocalDateTime now) {
        identifier = UUID.randomUUID().toString();
        this.code = code;
        applyDetails(details);
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
        updatedAt = now;
    }

    public void update(PublisherDetails details, LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), PublisherMessageKeys.NOT_FOUND);
        applyDetails(details);
        updatedAt = now;
    }

    private void applyDetails(PublisherDetails details) {
        name = details.name();
        description = details.description();
        scope = details.scope();
        deprecated = details.deprecated();
        settings = details.settings() == null ? null : details.settings().deepCopy();
    }

    public void inactivate(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), PublisherMessageKeys.NOT_FOUND);
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.inactive(), PublisherMessageKeys.RESTORE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.inactive(), PublisherMessageKeys.DELETE_INVALID);
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    public void requireRestorable() {
        requireLifecycle(LifecycleTypeCode.inactive(), PublisherMessageKeys.RESTORE_INVALID);
    }

    public void requireDeletable() {
        requireLifecycle(LifecycleTypeCode.inactive(), PublisherMessageKeys.DELETE_INVALID);
    }

    private void requireLifecycle(LifecycleTypeCode expected, String messageKey) {
        if (!expected.equals(lifecycle)) throw new ValidationException(messageKey);
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ResourceScopeTypeCode getScope() {
        return scope;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public JsonNode getSettings() {
        return settings == null ? null : settings.deepCopy();
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
