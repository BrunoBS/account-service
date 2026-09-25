package br.com.itau.portalmanager.workspace.feature.platform.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "platform_services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
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

    @OneToMany(mappedBy = "service")
    private Set<Feature> features = new LinkedHashSet<>();

    protected Service() {
    }

    public Service(String code, String name, String description, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.description = description;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getIdentifier() { return identifier; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Set<Feature> getFeatures() { return Collections.unmodifiableSet(features); }
}
