package br.com.portalmanager.platform.workspace.feature.shared.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shared_environment_mappings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_shared_mapping_destination",
            columnNames = { "participation_id", "destination_environment_identifier" }
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

    @Column(name = "source_environment_identifier", nullable = false, length = 36)
    private String sourceEnvironmentIdentifier;

    @Column(name = "destination_environment_identifier", nullable = false, length = 36)
    private String destinationEnvironmentIdentifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SharedEnvironmentMapping() {}

    public SharedEnvironmentMapping(String source, String destination, LocalDateTime now) {
        sourceEnvironmentIdentifier = source;
        destinationEnvironmentIdentifier = destination;
        createdAt = now;
        updatedAt = now;
    }

    void attach(SharedParticipation participation, LocalDateTime now) {
        this.participation = participation;
        updatedAt = now;
    }

    public String getSourceEnvironmentIdentifier() {
        return sourceEnvironmentIdentifier;
    }

    public String getDestinationEnvironmentIdentifier() {
        return destinationEnvironmentIdentifier;
    }
}
