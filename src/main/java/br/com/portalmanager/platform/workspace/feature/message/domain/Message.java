package br.com.portalmanager.platform.workspace.feature.message.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_messages_microservice_message_key", columnNames = {"microservice_id", "message_key"}),
        @UniqueConstraint(name = "uk_messages_microservice_message_code", columnNames = {"microservice_id", "code"})
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
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(length = 500)
    private String observation;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageTranslation> translations = new LinkedHashSet<>();

    protected Message() {
    }

    public Message(Long microserviceId, String messageKey, String code, Integer httpStatus, String observation, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.microserviceId = microserviceId;
        this.messageKey = messageKey;
        this.code = code;
        this.httpStatus = httpStatus;
        this.observation = observation;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(Long microserviceId, String messageKey, String code, Integer httpStatus, String observation, LocalDateTime now) {
        this.microserviceId = microserviceId;
        this.messageKey = messageKey;
        this.code = code;
        this.httpStatus = httpStatus;
        this.observation = observation;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    public boolean isInactive() {
        return LifecycleTypeCode.inactive().equals(lifecycle);
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
