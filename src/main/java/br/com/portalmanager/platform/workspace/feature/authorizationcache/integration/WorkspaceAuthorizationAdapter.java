package br.com.portalmanager.platform.workspace.feature.authorizationcache.integration;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceAuthorizationQueryService;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.port.WorkspaceAuthorizationPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceAuthorizationAdapter implements WorkspaceAuthorizationPort {
    private final WorkspaceAuthorizationQueryService queries;

    public WorkspaceAuthorizationAdapter(WorkspaceAuthorizationQueryService queries) {
        this.queries = queries;
    }

    @Override
    public List<String> findWorkspaceIdentifiers(String cursor) {
        return queries.findWorkspaceIdentifiers(cursor);
    }

    @Override
    public List<AuthorizationGroup> findGroups(String workspaceIdentifier) {
        return queries.findGroups(workspaceIdentifier).stream()
                .map(row -> new AuthorizationGroup(row.environmentIdentifier(),
                        row.applicationIdentifier(), row.authorizerGroup()))
                .toList();
    }
}
