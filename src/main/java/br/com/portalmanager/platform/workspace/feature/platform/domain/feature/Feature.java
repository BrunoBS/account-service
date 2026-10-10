package br.com.portalmanager.platform.workspace.feature.platform.domain.feature;

import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_features")
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "identifier", nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "microservice_id", nullable = false)
    private Microservice microservice;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "shareable", nullable = false)
    private boolean shareable;

    @Column(name = "settings", nullable = false, columnDefinition = "json")
    private String settings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Feature() {}

    public Feature(
        String code,
        String name,
        String description,
        Microservice microservice,
        String settings,
        LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.description = description;
        this.microservice = microservice;
        this.settings = settings;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
        microservice.attach(this);
    }

    public void update(String name, String description, String settings, LocalDateTime now) {
        this.name = name;
        this.description = description;
        this.settings = settings;
        this.updatedAt = now;
    }

    public void changeMicroservice(Microservice newService, LocalDateTime now) {
        if (microservice != null) microservice.detach(this);
        microservice = newService;
        newService.attach(this);
        updatedAt = now;
    }

    public void markUpdated(LocalDateTime now) {
        updatedAt = now;
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

    public void setShareable(boolean shareable, LocalDateTime now) {
        this.shareable = shareable;
        updatedAt = now;
    }

    public boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
    }

    public boolean isShareable() {
        return shareable;
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

    public Microservice getMicroservice() {
        return microservice;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public String getSettings() {
        return settings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
