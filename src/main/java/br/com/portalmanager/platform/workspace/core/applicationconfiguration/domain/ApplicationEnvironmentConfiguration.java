package br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "application_environment_configurations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_application_env_config_owner_environment",
                        columnNames = {"application_id", "environment_id"}))
public class ApplicationEnvironmentConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String identifier;

    @Column(name = "application_id", nullable = false, updatable = false)
    private Long applicationId;

    @Column(name = "environment_id", nullable = false, updatable = false)
    private Long environmentId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private JsonNode settings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ApplicationEnvironmentConfiguration() {}

    public ApplicationEnvironmentConfiguration(
            Long applicationId, Long environmentId, JsonNode settings, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.applicationId = Objects.requireNonNull(applicationId);
        this.environmentId = Objects.requireNonNull(environmentId);
        this.settings = Objects.requireNonNull(settings).deepCopy();
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public void updateSettings(JsonNode settings, LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active());
        this.settings = Objects.requireNonNull(settings).deepCopy();
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void inactivate(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active());
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.inactive());
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    private void requireLifecycle(LifecycleTypeCode expected) {
        if (!expected.equals(lifecycle))
            throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED);
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

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public JsonNode getSettings() {
        return settings.deepCopy();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
