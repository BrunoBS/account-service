package br.com.portalmanager.platform.workspace.feature.platform.domain.context;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_feature_contexts")
public class Context {

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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Context() {}

    public Context(String code, String name, String description, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.description = description;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, LocalDateTime now) {
        requireActive();
        this.name = name;
        this.description = description;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        if (!LifecycleTypeCode.inactive().equals(lifecycle)) throw new ValidationException(PlatformMessageKeys.CONTEXT_ACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        requireActive();
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void requireActive() {
        if (!isActive()) throw new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND);
    }

    public void markUpdated(LocalDateTime now) {
        updatedAt = now;
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
