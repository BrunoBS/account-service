package br.com.portalmanager.platform.workspace.feature.parameter.domain;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeCode;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "parameter_keys")
public class ParameterKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "microservice_id", nullable = false, updatable = false)
    private Long microserviceId;

    @Column(name = "parameter_key", nullable = false, length = 80, updatable = false)
    private String key;

    @Column(length = 200)
    private String description;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "type_code", nullable = false, length = 50))
    private ParameterKeyTypeCode type;

    @Column(nullable = false, columnDefinition = "json")
    private JsonNode value;

    @Column(name = "published_value", columnDefinition = "json")
    private JsonNode publishedValue;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "published_type_code", length = 50))
    private ParameterKeyTypeCode publishedType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "status_code", nullable = false, length = 50))
    private ParameterKeyStatusTypeCode status;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ParameterKey() {}

    public ParameterKey(
            Long microserviceId,
            String key,
            String description,
            ParameterKeyTypeCode type,
            JsonNode value,
            LocalDateTime now) {
        requireValue(value);
        this.identifier = UUID.randomUUID().toString();
        this.microserviceId = microserviceId;
        this.key = key;
        this.description = description;
        this.type = type;
        this.value = value.deepCopy();
        this.status = ParameterKeyStatusTypeCode.draft();
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String description, ParameterKeyTypeCode type, JsonNode value, LocalDateTime now) {
        requireValue(value);
        if (!Objects.equals(this.type, type) || !Objects.equals(this.value, value)) {
            status = ParameterKeyStatusTypeCode.draft();
        }
        this.description = description;
        this.type = type;
        this.value = value.deepCopy();
        this.updatedAt = now;
    }

    public void requirePublishable() {
        if (!active || !ParameterKeyStatusTypeCode.draft().equals(status)) {
            throw new ValidationException(ParameterKeyMessageKeys.PUBLISH_INVALID);
        }
    }

    public void publish(LocalDateTime now) {
        requirePublishable();
        publishedValue = value.deepCopy();
        publishedType = type;
        status = ParameterKeyStatusTypeCode.published();
        updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        if (active) return;
        active = true;
        status = ParameterKeyStatusTypeCode.draft();
        publishedValue = null;
        publishedType = null;
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        if (!active) return;
        active = false;
        status = ParameterKeyStatusTypeCode.draft();
        publishedValue = null;
        publishedType = null;
        updatedAt = now;
    }

    public void requireDeletable() {
        if (active) throw new ValidationException(ParameterKeyMessageKeys.DELETE_ACTIVE);
    }

    private void requireValue(JsonNode value) {
        if (value == null) throw new ValidationException(ParameterKeyMessageKeys.VALUE_REQUIRED);
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

    public Long getMicroserviceId() {
        return microserviceId;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public ParameterKeyTypeCode getType() {
        return type;
    }

    public JsonNode getValue() {
        return value.deepCopy();
    }

    public JsonNode getPublishedValue() {
        return publishedValue == null ? null : publishedValue.deepCopy();
    }

    public ParameterKeyTypeCode getPublishedType() {
        return publishedType;
    }

    public ParameterKeyStatusTypeCode getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
