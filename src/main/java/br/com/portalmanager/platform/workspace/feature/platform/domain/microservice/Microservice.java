package br.com.portalmanager.platform.workspace.feature.platform.domain.microservice;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
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

    @Column(name = "code", nullable = false, unique = true, length = 50)
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

    @OneToMany(mappedBy = "microservice")
    private Set<Feature> features = new LinkedHashSet<>();

    protected Microservice() {}

    public Microservice(String code, String name, String description, LocalDateTime now) {
        this(code, name, description, "{}", now);
    }

    public Microservice(String code, String name, String description, String settings, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.description = description;
        this.settings = settings;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, String settings, LocalDateTime now) {
        this.name = name;
        this.description = description;
        this.settings = settings;
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

    public void attach(Feature feature) {
        features.add(feature);
    }

    public void detach(Feature feature) {
        features.remove(feature);
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

    public Set<Feature> getFeatures() {
        return Collections.unmodifiableSet(features);
    }
}
