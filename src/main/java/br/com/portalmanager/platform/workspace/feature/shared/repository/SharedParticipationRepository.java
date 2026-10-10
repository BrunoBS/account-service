package br.com.portalmanager.platform.workspace.feature.shared.repository;

import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharedParticipationRepository extends JpaRepository<SharedParticipation, Long> {
    Optional<SharedParticipation> findByIdentifier(String identifier);
    Optional<SharedParticipation> findByIdentifierAndContractIdentifier(String identifier, String contractIdentifier);
    Optional<SharedParticipation> findByContractIdAndIdentifier(Long contractId, String identifier);
    Optional<SharedParticipation> findByContractIdAndParticipantApplicationId(Long contractId, Long applicationId);
    Optional<SharedParticipation> findByContractIdAndIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
        Long contractId,
        String identifier,
        Long workspaceId,
        Long applicationId
    );
    Optional<SharedParticipation> findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
        String identifier,
        Long workspaceId,
        Long applicationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select participation from SharedParticipation participation where participation.contract.id = :contractId and participation.identifier = :identifier"
    )
    Optional<SharedParticipation> findOwnerParticipationForUpdate(
        @Param("contractId") Long contractId,
        @Param("identifier") String identifier
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select participation from SharedParticipation participation where participation.identifier = :identifier and participation.participantWorkspaceId = :workspaceId and participation.participantApplicationId = :applicationId"
    )
    Optional<SharedParticipation> findParticipantParticipationForUpdate(
        @Param("identifier") String identifier,
        @Param("workspaceId") Long workspaceId,
        @Param("applicationId") Long applicationId
    );

    @EntityGraph(attributePaths = { "contract", "mappings" })
    List<SharedParticipation> findByIdIn(Collection<Long> participationIds);

    @Query(
        value = """
        SELECT participation.id
        FROM shared_participations participation
        JOIN applications application ON application.id = participation.participant_application_id
         AND application.workspace_id = participation.participant_workspace_id
        WHERE participation.contract_id = :contractId
         AND (:status IS NULL OR participation.status_code = :status)
         AND (:applicationIdentifier IS NULL OR application.identifier = :applicationIdentifier)
         AND (:participantName IS NULL OR LOCATE(:participantName, LOWER(application.name)) > 0)
        ORDER BY participation.created_at, participation.id
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM shared_participations participation
        JOIN applications application ON application.id = participation.participant_application_id
         AND application.workspace_id = participation.participant_workspace_id
        WHERE participation.contract_id = :contractId
         AND (:status IS NULL OR participation.status_code = :status)
         AND (:applicationIdentifier IS NULL OR application.identifier = :applicationIdentifier)
         AND (:participantName IS NULL OR LOCATE(:participantName, LOWER(application.name)) > 0)
        """,
        nativeQuery = true
    )
    Page<Long> findOwnerParticipationIds(
        @Param("contractId") Long contractId,
        @Param("status") String status,
        @Param("applicationIdentifier") String applicationIdentifier,
        @Param("participantName") String participantName,
        Pageable pageable
    );

    @Query(
        value = """
        SELECT participation.id
        FROM shared_participations participation
        JOIN shared_contracts contract ON contract.id = participation.contract_id
        WHERE participation.participant_workspace_id = :workspaceId
         AND participation.participant_application_id = :applicationId
         AND (:status IS NULL OR participation.status_code = :status)
         AND (:contractIdentifier IS NULL OR contract.identifier = :contractIdentifier)
        ORDER BY participation.created_at DESC, participation.id DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM shared_participations participation
        JOIN shared_contracts contract ON contract.id = participation.contract_id
        WHERE participation.participant_workspace_id = :workspaceId
         AND participation.participant_application_id = :applicationId
         AND (:status IS NULL OR participation.status_code = :status)
         AND (:contractIdentifier IS NULL OR contract.identifier = :contractIdentifier)
        """,
        nativeQuery = true
    )
    Page<Long> findParticipantParticipationIds(
        @Param("workspaceId") Long workspaceId,
        @Param("applicationId") Long applicationId,
        @Param("status") String status,
        @Param("contractIdentifier") String contractIdentifier,
        Pageable pageable
    );
}
