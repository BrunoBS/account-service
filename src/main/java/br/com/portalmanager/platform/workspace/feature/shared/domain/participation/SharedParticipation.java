package br.com.portalmanager.platform.workspace.feature.shared.domain.participation;

import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "shared_participations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_participations_contract_application",
        columnNames = { "contract_id", "participant_application_id" }
    )
)
public class SharedParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private SharedContract contract;

    @Column(name = "participant_workspace_id", nullable = false)
    private Long participantWorkspaceId;

    @Column(name = "participant_application_id", nullable = false)
    private Long participantApplicationId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "status_code", nullable = false, length = 30))
    private ShareStatusTypeCode status;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "publication_mode_code", length = 30))
    private PublicationModeTypeCode publicationMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "participation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SharedEnvironmentMapping> mappings = new LinkedHashSet<>();

    protected SharedParticipation() {}

    public SharedParticipation(SharedContract contract, Long workspaceId, Long applicationId, LocalDateTime now) {
        identifier = UUID.randomUUID().toString();
        this.contract = contract;
        participantWorkspaceId = workspaceId;
        participantApplicationId = applicationId;
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.PENDING);
        createdAt = now;
        updatedAt = now;
    }

    public void approve(PublicationModeTypeCode mode, LocalDateTime now) {
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.APPROVED);
        publicationMode = mode;
        updatedAt = now;
    }

    public void reject(LocalDateTime now) {
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.REJECTED);
        updatedAt = now;
    }

    public void revoke(LocalDateTime now) {
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.REVOKED);
        updatedAt = now;
    }

    public void requestAgain(LocalDateTime now) {
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.PENDING);
        publicationMode = null;
        mappings.clear();
        updatedAt = now;
    }

    public void configure(
        PublicationModeTypeCode publicationMode,
        Set<SharedEnvironmentMapping> mappings,
        LocalDateTime now
    ) {
        this.publicationMode = publicationMode;
        replaceMappings(mappings, now);
    }

    public void replaceMappings(Set<SharedEnvironmentMapping> replacements, LocalDateTime now) {
        Map<Long, SharedEnvironmentMapping> existingByDestination = mappings
            .stream()
            .collect(Collectors.toMap(SharedEnvironmentMapping::getDestinationEnvironmentId, mapping -> mapping));
        Set<Long> destinationIds = replacements
            .stream()
            .map(SharedEnvironmentMapping::getDestinationEnvironmentId)
            .collect(Collectors.toSet());
        mappings.removeIf(mapping -> !destinationIds.contains(mapping.getDestinationEnvironmentId()));
        for (SharedEnvironmentMapping replacement : replacements) {
            SharedEnvironmentMapping existing = existingByDestination.get(replacement.getDestinationEnvironmentId());
            if (existing == null) {
                replacement.attach(this, now);
                mappings.add(replacement);
            } else {
                existing.updateSource(replacement.getSourceEnvironmentId(), now);
            }
        }
        updatedAt = now;
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

    public SharedContract getContract() {
        return contract;
    }

    public Long getParticipantWorkspaceId() {
        return participantWorkspaceId;
    }

    public Long getParticipantApplicationId() {
        return participantApplicationId;
    }

    public ShareStatusTypeCode getStatus() {
        return status;
    }

    public PublicationModeTypeCode getPublicationMode() {
        return publicationMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<SharedEnvironmentMapping> getMappings() {
        return mappings;
    }
}
