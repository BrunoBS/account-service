package br.com.portalmanager.platform.workspace.feature.shared.repository;

import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharedContractRepository extends JpaRepository<SharedContract, Long> {
    @Query(
        value = "SELECT id FROM shared_contracts WHERE feature_id = :featureId LIMIT 1 FOR SHARE",
        nativeQuery = true
    )
    Optional<Long> findFirstContractIdByFeatureForShare(@Param("featureId") Long featureId);

    /** A locking current read avoids a stale repeatable-read snapshot after acquiring the application lock. */
    @Query(
        value = "SELECT id FROM shared_contracts WHERE owner_application_id = :applicationId LIMIT 1 FOR UPDATE",
        nativeQuery = true
    )
    Optional<Long> findFirstContractIdByOwnerApplicationForUpdate(@Param("applicationId") Long applicationId);

    @Query(
        value = """
        SELECT contract.*
        FROM shared_contracts contract
        JOIN applications application ON application.id = contract.owner_application_id AND application.workspace_id = contract.owner_workspace_id
        JOIN workspaces workspace ON workspace.id = contract.owner_workspace_id
        JOIN platform_features feature ON feature.id = contract.feature_id
        JOIN platform_microservices microservice ON microservice.id = feature.microservice_id
        WHERE contract.lifecycle_code = :lifecycle
         AND application.lifecycle_code = :lifecycle AND workspace.lifecycle_code = :lifecycle
         AND application.application_scope_code = 'SHARED'
         AND feature.lifecycle_code = :lifecycle AND feature.shareable = true AND microservice.lifecycle_code = :lifecycle
         AND contract.owner_application_id <> :participantApplicationId
         AND (:ownerWorkspaceIdentifier IS NULL OR workspace.identifier = :ownerWorkspaceIdentifier)
         AND (:ownerApplicationIdentifier IS NULL OR application.identifier = :ownerApplicationIdentifier)
        ORDER BY contract.id
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM shared_contracts contract
        JOIN applications application ON application.id = contract.owner_application_id AND application.workspace_id = contract.owner_workspace_id
        JOIN workspaces workspace ON workspace.id = contract.owner_workspace_id
        JOIN platform_features feature ON feature.id = contract.feature_id
        JOIN platform_microservices microservice ON microservice.id = feature.microservice_id
        WHERE contract.lifecycle_code = :lifecycle
         AND application.lifecycle_code = :lifecycle AND workspace.lifecycle_code = :lifecycle
         AND application.application_scope_code = 'SHARED'
         AND feature.lifecycle_code = :lifecycle AND feature.shareable = true AND microservice.lifecycle_code = :lifecycle
         AND contract.owner_application_id <> :participantApplicationId
         AND (:ownerWorkspaceIdentifier IS NULL OR workspace.identifier = :ownerWorkspaceIdentifier)
         AND (:ownerApplicationIdentifier IS NULL OR application.identifier = :ownerApplicationIdentifier)
        """,
        nativeQuery = true
    )
    Page<SharedContract> findAvailableContracts(
        @Param("lifecycle") String lifecycle,
        @Param("participantApplicationId") Long participantApplicationId,
        @Param("ownerWorkspaceIdentifier") String ownerWorkspaceIdentifier,
        @Param("ownerApplicationIdentifier") String ownerApplicationIdentifier,
        Pageable pageable
    );

    Page<SharedContract> findByOwnerWorkspaceIdAndOwnerApplicationId(
        Long workspaceId,
        Long applicationId,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select contract from SharedContract contract where contract.identifier = :identifier and contract.ownerWorkspaceId = :workspaceId and contract.ownerApplicationId = :applicationId"
    )
    Optional<SharedContract> findOwnedContractForUpdate(
        @Param("identifier") String identifier,
        @Param("workspaceId") Long workspaceId,
        @Param("applicationId") Long applicationId
    );

    @Query(value = "SELECT lifecycle_code FROM shared_contracts WHERE id = :contractId FOR SHARE", nativeQuery = true)
    Optional<String> findLifecycleForShare(@Param("contractId") Long contractId);

    Optional<SharedContract> findByIdentifierAndOwnerWorkspaceIdAndOwnerApplicationId(
        String identifier,
        Long workspaceId,
        Long applicationId
    );
    Optional<SharedContract> findByIdentifierAndLifecycleValue(String identifier, String lifecycle);

    @Query(
        value = "SELECT id FROM shared_contracts WHERE owner_workspace_id = :workspaceId AND owner_application_id = :applicationId AND feature_id = :featureId LIMIT 1 FOR UPDATE",
        nativeQuery = true
    )
    Optional<Long> findContractIdByOwnerAndFeatureForUpdate(
        @Param("workspaceId") Long workspaceId,
        @Param("applicationId") Long applicationId,
        @Param("featureId") Long featureId
    );
}
