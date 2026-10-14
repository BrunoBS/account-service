package br.com.portalmanager.platform.workspace.feature.platform.domain.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_microservices")
public class Microservice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "identifier", nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "code", nullable = false, unique = true, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "settings", nullable = false, columnDefinition = "json")
    private String settings;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Microservice() {}

    public Microservice(String code, String name, String description, LocalDateTime now) {
        this(code, name, description, "{}", now);
    }

    public Microservice(String code, String name, String description, String settings, LocalDateTime now) {
        this(code, new MicroserviceDetails(name, description, settings), now);
    }

    public Microservice(String code, MicroserviceDetails details, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = details.name();
        this.description = details.description();
        this.settings = details.settings();
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(MicroserviceDetails details, LocalDateTime now) {
        requireActive();
        this.name = details.name();
        this.description = details.description();
        this.settings = details.settings();
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        if (!LifecycleTypeCode.inactive().equals(lifecycle)) throw new ValidationException(PlatformMessageKeys.MICROSERVICE_ACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        requireActive();
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void requireActive() {
        if (!isActive()) throw new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND);
    }

    public boolean isActive() {
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSettings() {
        return settings;
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
