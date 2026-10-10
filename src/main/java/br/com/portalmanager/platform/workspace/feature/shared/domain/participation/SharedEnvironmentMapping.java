package br.com.portalmanager.platform.workspace.feature.shared.domain.participation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shared_environment_mappings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_shared_mapping_destination",
            columnNames = { "participation_id", "destination_environment_id" }
        ),
    }
)
public class SharedEnvironmentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private SharedParticipation participation;

    @Column(name = "source_environment_id", nullable = false)
    private Long sourceEnvironmentId;

    @Column(name = "destination_environment_id", nullable = false)
    private Long destinationEnvironmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SharedEnvironmentMapping() {}

    public SharedEnvironmentMapping(Long sourceEnvironmentId, Long destinationEnvironmentId, LocalDateTime now) {
        this.sourceEnvironmentId = sourceEnvironmentId;
        this.destinationEnvironmentId = destinationEnvironmentId;
        createdAt = now;
        updatedAt = now;
    }

    void attach(SharedParticipation participation, LocalDateTime now) {
        this.participation = participation;
        updatedAt = now;
    }

    void updateSource(Long sourceEnvironmentId, LocalDateTime now) {
        this.sourceEnvironmentId = sourceEnvironmentId;
        updatedAt = now;
    }

    public Long getSourceEnvironmentId() {
        return sourceEnvironmentId;
    }

    public Long getDestinationEnvironmentId() {
        return destinationEnvironmentId;
    }
}
