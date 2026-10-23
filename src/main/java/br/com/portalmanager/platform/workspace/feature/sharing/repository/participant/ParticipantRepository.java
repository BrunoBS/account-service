package br.com.portalmanager.platform.workspace.feature.sharing.repository.participant;

import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.contract.ContractRepository.ApplicationLink;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @Query(
            "select distinct p.participantApplicationId as applicationId, p.participantWorkspaceId"
                    + " as workspaceId from Participant p where p.participantApplicationId in :ids")
    List<ApplicationLink> findApplicationLinks(@Param("ids") Collection<Long> ids);

    boolean existsByParticipantWorkspaceId(Long workspaceId);

    @Query(
            "select distinct mapping.sourceEnvironmentId from Participant p join p.mappings mapping"
                    + " where mapping.sourceEnvironmentId in :ids")
    List<Long> findSourceEnvironmentLinks(@Param("ids") Collection<Long> ids);

    @Query(
            "select distinct mapping.destinationEnvironmentId from Participant p join p.mappings"
                    + " mapping where mapping.destinationEnvironmentId in :ids")
    List<Long> findDestinationEnvironmentLinks(@Param("ids") Collection<Long> ids);

    Optional<Participant> findByIdentifier(String identifier);

    Optional<Participant> findByIdentifierAndContractIdentifier(
            String identifier, String contractIdentifier);

    Optional<Participant> findByContractIdAndIdentifier(Long contractId, String identifier);

    Optional<Participant> findByContractIdAndParticipantApplicationId(
            Long contractId, Long applicationId);

    Optional<Participant>
            findByContractIdAndIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
                    Long contractId, String identifier, Long workspaceId, Long applicationId);

    Optional<Participant> findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
            String identifier, Long workspaceId, Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select participant from Participant participant where participant.contract.id ="
                    + " :contractId and participant.identifier = :identifier")
    Optional<Participant> findOwnerParticipantForUpdate(
            @Param("contractId") Long contractId, @Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select participant from Participant participant where participant.identifier ="
                    + " :identifier and participant.participantWorkspaceId = :workspaceId and"
                    + " participant.participantApplicationId = :applicationId")
    Optional<Participant> findParticipantParticipantForUpdate(
            @Param("identifier") String identifier,
            @Param("workspaceId") Long workspaceId,
            @Param("applicationId") Long applicationId);

    @EntityGraph(attributePaths = {"contract", "mappings"})
    List<Participant> findByIdIn(Collection<Long> participantIds);

    @Query(
            value =
                    """
                    SELECT participant.id
                    FROM resource_sharing_participants participant
                    JOIN applications application ON application.id = participant.participant_application_id
                     AND application.workspace_id = participant.participant_workspace_id
                    WHERE participant.contract_id = :contractId
                     AND (:status IS NULL OR participant.status_code = :status)
                     AND (:applicationIdentifier IS NULL OR application.identifier = :applicationIdentifier)
                     AND (:participantName IS NULL OR LOCATE(:participantName, LOWER(application.name)) > 0)
                    ORDER BY participant.created_at, participant.id
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM resource_sharing_participants participant
                    JOIN applications application ON application.id = participant.participant_application_id
                     AND application.workspace_id = participant.participant_workspace_id
                    WHERE participant.contract_id = :contractId
                     AND (:status IS NULL OR participant.status_code = :status)
                     AND (:applicationIdentifier IS NULL OR application.identifier = :applicationIdentifier)
                     AND (:participantName IS NULL OR LOCATE(:participantName, LOWER(application.name)) > 0)
                    """,
            nativeQuery = true)
    Page<Long> findOwnerParticipantIds(
            @Param("contractId") Long contractId,
            @Param("status") String status,
            @Param("applicationIdentifier") String applicationIdentifier,
            @Param("participantName") String participantName,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT participant.id
                    FROM resource_sharing_participants participant
                    JOIN resource_sharing_contracts contract ON contract.id = participant.contract_id
                    WHERE participant.participant_workspace_id = :workspaceId
                     AND participant.participant_application_id = :applicationId
                     AND (:status IS NULL OR participant.status_code = :status)
                     AND (:contractIdentifier IS NULL OR contract.identifier = :contractIdentifier)
                    ORDER BY participant.created_at DESC, participant.id DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM resource_sharing_participants participant
                    JOIN resource_sharing_contracts contract ON contract.id = participant.contract_id
                    WHERE participant.participant_workspace_id = :workspaceId
                     AND participant.participant_application_id = :applicationId
                     AND (:status IS NULL OR participant.status_code = :status)
                     AND (:contractIdentifier IS NULL OR contract.identifier = :contractIdentifier)
                    """,
            nativeQuery = true)
    Page<Long> findParticipantParticipantIds(
            @Param("workspaceId") Long workspaceId,
            @Param("applicationId") Long applicationId,
            @Param("status") String status,
            @Param("contractIdentifier") String contractIdentifier,
            Pageable pageable);
}
