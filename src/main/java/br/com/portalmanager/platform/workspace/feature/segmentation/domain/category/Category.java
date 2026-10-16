package br.com.portalmanager.platform.workspace.feature.segmentation.domain.category;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "category",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_category_code",
                        columnNames = {"segment_id", "code"}))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(nullable = false, length = 100, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false, updatable = false)
    private Segment segment;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Category() {}

    public Category(Segment segment, String code, String name, LocalDateTime now) {
        segment.requireActiveForCategoryCreation();
        this.segment = segment;
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, LocalDateTime now) {
        this.name = name;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        if (!LifecycleTypeCode.inactive().equals(lifecycle))
            throw new ValidationException(SegmentationMessageKeys.CATEGORY_ACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        if (!isActive())
            throw new ValidationException(SegmentationMessageKeys.CATEGORY_INACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
    }

    public Segment getSegment() {
        return segment;
    }

    public boolean isAvailable() {
        return isActive() && segment.isActive();
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
