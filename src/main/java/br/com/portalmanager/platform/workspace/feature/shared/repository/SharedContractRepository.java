package br.com.portalmanager.platform.workspace.feature.shared.repository;

import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SharedContractRepository extends JpaRepository<SharedContract, Long> {
    Optional<SharedContract> findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
            String identifier, String workspaceIdentifier, String applicationIdentifier);
    List<SharedContract> findByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierAndLifecycleValueOrderById(
            String workspaceIdentifier, String applicationIdentifier, String lifecycle);
    List<SharedContract> findByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierOrderById(
            String workspaceIdentifier, String applicationIdentifier);
    List<SharedContract> findByLifecycleValueOrderById(String lifecycle);
    Optional<SharedContract> findByIdentifierAndLifecycleValue(String identifier, String lifecycle);
    boolean existsByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierAndName(
            String workspaceIdentifier, String applicationIdentifier, String name);
    boolean existsByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierAndNameAndIdNot(
            String workspaceIdentifier, String applicationIdentifier, String name, Long id);
}
