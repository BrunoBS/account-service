package br.com.itau.portalmanager.workspace.feature.message.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_messages_service_message_key",
                        columnNames = {"service_code", "message_key"}
                ),
                @UniqueConstraint(
                        name = "uk_messages_service_code",
                        columnNames = {"service_code", "code"}
                )
        }
)
public class Message {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "message_key", nullable = false, length = 255)
    private String messageKey;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Column(name = "lifecycle_code", nullable = false, length = 50)
    private String lifecycleCode;

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

    public Message(
            String serviceCode,
            String messageKey,
            String code,
            Integer httpStatus,
            String observation,
            LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.serviceCode = serviceCode;
        this.messageKey = messageKey;
        this.code = code;
        this.httpStatus = httpStatus;
        this.observation = observation;
        this.lifecycleCode = ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String serviceCode,
            String messageKey,
            String code,
            Integer httpStatus,
            String observation,
            LocalDateTime now
    ) {
        this.serviceCode = serviceCode;
        this.messageKey = messageKey;
        this.code = code;
        this.httpStatus = httpStatus;
        this.observation = observation;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        this.lifecycleCode = ACTIVE;
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        this.lifecycleCode = INACTIVE;
        this.updatedAt = now;
    }

    public boolean isInactive() {
        return INACTIVE.equals(lifecycleCode);
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public String getServiceCode() { return serviceCode; }
    public String getMessageKey() { return messageKey; }
    public String getCode() { return code; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getLifecycleCode() { return lifecycleCode; }
    public String getObservation() { return observation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
