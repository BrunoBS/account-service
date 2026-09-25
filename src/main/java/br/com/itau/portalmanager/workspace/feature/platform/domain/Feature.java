package br.com.itau.portalmanager.workspace.feature.platform.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "platform_features")
public class Feature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Long id;
    @Column(name = "identifier", nullable = false, unique = true, length = 36, updatable = false) private String identifier;
    @Column(name = "code", nullable = false, unique = true, length = 50) private String code;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "description", length = 500) private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id", nullable = false) private Service service;
    @ManyToMany
    @JoinTable(name = "platform_feature_scopes", joinColumns = @JoinColumn(name = "feature_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_scope_code"))
    private Set<FeatureScopeType> scopes = new LinkedHashSet<>();
    @Embedded @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;
    @Column(name = "settings", nullable = false, columnDefinition = "json") private String settings;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected Feature() {}

    public Feature(String code, String name, String description, Service service, String settings, LocalDateTime now) {
        if (service == null || !service.isActive()) throw new IllegalArgumentException("Feature requires an active service");
        this.identifier = UUID.randomUUID().toString();
        this.code = code; this.name = name; this.description = description; this.service = service;
        this.settings = settings; this.lifecycle = LifecycleTypeCode.active(); this.createdAt = now; this.updatedAt = now;
        service.attach(this);
    }

    public void update(String name, String description, String settings, LocalDateTime now) {
        this.name = name; this.description = description; this.settings = settings; this.updatedAt = now;
    }
    public void changeService(Service newService, LocalDateTime now) {
        if (newService == null || !newService.isActive()) throw new IllegalArgumentException("Feature requires an active service");
        if (service != null) service.detach(this);
        service = newService; newService.attach(this); updatedAt = now;
    }
    public void addScope(FeatureScopeType scope) {
        if (scope == null) throw new IllegalArgumentException("Feature scope is required");
        scopes.add(scope);
    }
    public void removeScope(FeatureScopeType scope) { scopes.remove(scope); }
    public void activate(LocalDateTime now) { lifecycle = LifecycleTypeCode.active(); updatedAt = now; }
    public void inactivate(LocalDateTime now) { lifecycle = LifecycleTypeCode.inactive(); updatedAt = now; }
    public void quarantine(LocalDateTime now) { lifecycle = LifecycleTypeCode.quarantined(); updatedAt = now; }

    public Long getId() { return id; }
    public String getIdentifier() { return identifier; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Service getService() { return service; }
    public Set<FeatureScopeType> getScopes() { return Collections.unmodifiableSet(scopes); }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public String getSettings() { return settings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
