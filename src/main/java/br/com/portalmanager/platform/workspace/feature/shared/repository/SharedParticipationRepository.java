package br.com.portalmanager.platform.workspace.feature.shared.repository;

import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedParticipation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharedParticipationRepository extends JpaRepository<SharedParticipation, Long> {
    Optional<SharedParticipation> findByIdentifier(String identifier);
    Optional<SharedParticipation> findByIdentifierAndContractIdentifier(String identifier, String contractIdentifier);
    Optional<SharedParticipation> findByContractIdAndIdentifier(Long contractId, String identifier);
    Optional<SharedParticipation> findByContractIdAndParticipantApplicationIdentifier(
        Long contractId,
        String applicationIdentifier
    );
    Optional<SharedParticipation> findByContractIdAndIdentifierAndParticipantWorkspaceIdentifierAndParticipantApplicationIdentifier(
        Long contractId,
        String identifier,
        String workspaceIdentifier,
        String applicationIdentifier
    );
    Optional<SharedParticipation> findByIdentifierAndParticipantWorkspaceIdentifierAndParticipantApplicationIdentifier(
        String identifier,
        String workspaceIdentifier,
        String applicationIdentifier
    );

    @Query(
        "select p from SharedParticipation p where p.contract.id = :contractId and p.status.value in :statuses order by p.createdAt"
    )
    List<SharedParticipation> findByContractAndStatuses(
        @Param("contractId") Long contractId,
        @Param("statuses") List<String> statuses
    );

    List<SharedParticipation> findByParticipantWorkspaceIdentifierAndParticipantApplicationIdentifierOrderByCreatedAtDesc(
        String workspaceIdentifier,
        String applicationIdentifier
    );
}
