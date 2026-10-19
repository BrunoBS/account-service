package br.com.portalmanager.platform.workspace.feature.onboarding.repository;

import br.com.portalmanager.platform.workspace.feature.onboarding.domain.WorkspaceOnboardingCompletion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkspaceOnboardingCompletionRepository
        extends JpaRepository<WorkspaceOnboardingCompletion, Long> {

    List<WorkspaceOnboardingCompletion> findByWorkspaceId(Long workspaceId);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO workspace_onboarding_completions
                        (workspace_id, onboarding_phase_code, requester, completed_at)
                    VALUES (:workspaceId, :phaseCode, :requester, :completedAt)
                    ON DUPLICATE KEY UPDATE id = id
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("workspaceId") Long workspaceId,
            @Param("phaseCode") String phaseCode,
            @Param("requester") String requester,
            @Param("completedAt") LocalDateTime completedAt);
}
