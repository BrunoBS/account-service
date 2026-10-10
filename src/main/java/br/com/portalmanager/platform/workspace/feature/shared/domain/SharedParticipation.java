package br.com.portalmanager.platform.workspace.feature.shared.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

@Entity
@Table(name = "shared_participations", uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_participations_contract_application",
        columnNames = {"contract_id", "participant_application_identifier"}))
public class SharedParticipation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @Column(nullable = false, unique = true, length = 36, updatable = false) private String identifier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false) private SharedContract contract;
    @Column(name = "participant_workspace_identifier", nullable = false, length = 36) private String participantWorkspaceIdentifier;
    @Column(name = "participant_application_identifier", nullable = false, length = 36) private String participantApplicationIdentifier;
    @Embedded @AttributeOverride(name = "value", column = @Column(name = "status_code", nullable = false, length = 30))
    private ShareStatusTypeCode status;
    @Embedded @AttributeOverride(name = "value", column = @Column(name = "publication_mode_code", length = 30))
    private PublicationModeTypeCode publicationMode;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "participation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SharedEnvironmentMapping> mappings = new LinkedHashSet<>();

    protected SharedParticipation() {}
    public SharedParticipation(SharedContract contract, String workspaceIdentifier, String applicationIdentifier, LocalDateTime now) {
        identifier = UUID.randomUUID().toString(); this.contract = contract;
        participantWorkspaceIdentifier = workspaceIdentifier; participantApplicationIdentifier = applicationIdentifier;
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.PENDING); createdAt = now; updatedAt = now;
    }
    public void approve(PublicationModeTypeCode mode, LocalDateTime now) { status = ShareStatusTypeCode.of(ShareStatusTypeEnum.APPROVED); publicationMode = mode; updatedAt = now; }
    public void reject(LocalDateTime now) { status = ShareStatusTypeCode.of(ShareStatusTypeEnum.REJECTED); updatedAt = now; }
    public void revoke(LocalDateTime now) { status = ShareStatusTypeCode.of(ShareStatusTypeEnum.REVOKED); updatedAt = now; }
    public void resubmit(LocalDateTime now) {
        status = ShareStatusTypeCode.of(ShareStatusTypeEnum.PENDING);
        publicationMode = null;
        mappings.clear();
        updatedAt = now;
    }
    public void changePublicationMode(PublicationModeTypeCode mode, LocalDateTime now) { publicationMode = mode; updatedAt = now; }
    public void replaceMappings(Set<SharedEnvironmentMapping> replacements, LocalDateTime now) {
        mappings.clear();
        for (SharedEnvironmentMapping mapping : replacements) { mapping.attach(this, now); mappings.add(mapping); }
        updatedAt = now;
    }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public SharedContract getContract() { return contract; }
    public String getParticipantWorkspaceIdentifier() { return participantWorkspaceIdentifier; }
    public String getParticipantApplicationIdentifier() { return participantApplicationIdentifier; }
    public ShareStatusTypeCode getStatus() { return status; }
    public PublicationModeTypeCode getPublicationMode() { return publicationMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Set<SharedEnvironmentMapping> getMappings() { return mappings; }
}
