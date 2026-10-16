package br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "messages",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_messages_microservice_message_key",
                    columnNames = {"microservice_id", "message_key"}),
            @UniqueConstraint(
                    name = "uk_messages_microservice_message_code",
                    columnNames = {"microservice_id", "code"}),
        })
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "microservice_id", nullable = false)
    private Long microserviceId;

    @Column(name = "message_key", nullable = false, length = 255)
    private String messageKey;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(length = 500)
    private String observation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Message() {}

    public Message(Long microserviceId, MessageDetails details, LocalDateTime now) {
        identifier = UUID.randomUUID().toString();
        this.microserviceId = microserviceId;
        applyDetails(details);
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
        updatedAt = now;
    }

    public void update(Long microserviceId, MessageDetails details, LocalDateTime now) {
        requireActiveForUpdate();
        this.microserviceId = microserviceId;
        applyDetails(details);
        updatedAt = now;
    }

    private void applyDetails(MessageDetails details) {
        messageKey = details.messageKey();
        code = details.code();
        httpStatus = details.httpStatus();
        observation = details.observation();
    }

    public void activate(LocalDateTime now) {
        if (!isInactive()) throw new ValidationException(MessageMessageKeys.ACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        if (!isActive()) throw new ValidationException(MessageMessageKeys.INACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void requireInactiveForDeletion() {
        if (!isInactive()) throw new ValidationException(MessageMessageKeys.DELETE_INVALID);
    }

    public boolean isInactive() {
        return LifecycleTypeCode.inactive().equals(lifecycle);
    }

    public void requireActiveForUpdate() {
        if (!isActive()) throw new ValidationException(MessageMessageKeys.UPDATE_INVALID);
    }

    public void requireActiveForTranslator() {
        if (!isActive())
            throw new ValidationException(MessageMessageKeys.TRANSLATOR_PARENT_INACTIVE);
    }

    private boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
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

    public String getMessageKey() {
        return messageKey;
    }

    public String getCode() {
        return code;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public String getObservation() {
        return observation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
