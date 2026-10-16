package br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "resource_sharing_environment_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_resource_sharing_mapping_destination",
                    columnNames = {"participant_id", "destination_environment_id"}),
        })
public class EnvironmentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "source_environment_id", nullable = false)
    private Long sourceEnvironmentId;

    @Column(name = "destination_environment_id", nullable = false)
    private Long destinationEnvironmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EnvironmentMapping() {}

    public EnvironmentMapping(
            Long sourceEnvironmentId, Long destinationEnvironmentId, LocalDateTime now) {
        this.sourceEnvironmentId = sourceEnvironmentId;
        this.destinationEnvironmentId = destinationEnvironmentId;
        createdAt = now;
        updatedAt = now;
    }

    void attach(Participant participant, LocalDateTime now) {
        this.participant = participant;
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
