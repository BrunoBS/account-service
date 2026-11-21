package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkspaceAuthorizationQueryService {
    private final WorkspaceRepository repository;

    public WorkspaceAuthorizationQueryService(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public List<String> findWorkspaceIdentifiers(String cursor) {
        return repository.findAuthorizationRefreshWorkspaceIdentifiers(cursor);
    }

    public List<AuthorizationGroup> findGroups(String workspaceIdentifier) {
        return repository.findAuthorizationGroups(workspaceIdentifier).stream()
                .map(row -> new AuthorizationGroup(row.getEnvironmentIdentifier(),
                        row.getApplicationIdentifier(), row.getAuthorizerGroup()))
                .toList();
    }

    public record AuthorizationGroup(String environmentIdentifier, String applicationIdentifier,
                                     String authorizerGroup) {}
}
