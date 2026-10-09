package br.com.portalmanager.platform.workspace.foundation.schema.facade;

import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaVersionRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaVersionResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaCommandService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.version.SchemaVersionCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceSchemaFacade {

    private final SchemaCommandService commandService;
    private final SchemaQueryService queryService;
    private final SchemaVersionCommandService versionCommandService;

    public WorkspaceSchemaFacade(
            SchemaCommandService commandService,
            SchemaQueryService queryService,
            SchemaVersionCommandService versionCommandService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.versionCommandService = versionCommandService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ResponseEntity<SchemaResponse> create(
            String workspaceIdentifier,
            CreateSchemaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaResponse.from(
                        commandService.createWorkspace(request.toWorkspaceInput(workspaceIdentifier))
                ));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public List<SchemaResponse> findAll(String workspaceIdentifier) {
        return queryService.findWorkspace(workspaceIdentifier).stream()
                .map(SchemaResponse::from)
                .toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public SchemaResponse findByIdentifier(
            String workspaceIdentifier,
            String identifier
    ) {
        return SchemaResponse.from(queryService.findWorkspaceByIdentifier(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public SchemaResponse update(
            String workspaceIdentifier,
            String identifier,
            UpdateSchemaRequest request
    ) {
        return SchemaResponse.from(commandService.updateWorkspace(workspaceIdentifier, identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public SchemaResponse activate(
            String workspaceIdentifier,
            String identifier
    ) {
        return SchemaResponse.from(commandService.activateWorkspace(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public SchemaResponse inactivate(
            String workspaceIdentifier,
            String identifier
    ) {
        return SchemaResponse.from(commandService.inactivateWorkspace(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> quarantine(
            String workspaceIdentifier,
            String identifier
    ) {
        commandService.quarantineWorkspace(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ResponseEntity<SchemaVersionResponse> createVersion(
            String workspaceIdentifier,
            String identifier,
            CreateSchemaVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaVersionResponse.from(
                        versionCommandService.createWorkspaceDraft(workspaceIdentifier, identifier, request.toInput())
                ));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> deleteDraft(
            String workspaceIdentifier,
            String identifier,
            String versionIdentifier
    ) {
        versionCommandService.deleteWorkspaceDraft(workspaceIdentifier, identifier, versionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public List<SchemaVersionResponse> findVersions(
            String workspaceIdentifier,
            String identifier
    ) {
        return queryService.findWorkspaceVersions(workspaceIdentifier, identifier).stream()
                .map(SchemaVersionResponse::from)
                .toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public SchemaVersionResponse publish(
            String workspaceIdentifier,
            String identifier,
            String versionIdentifier
    ) {
        return SchemaVersionResponse.from(
                versionCommandService.publishWorkspace(workspaceIdentifier, identifier, versionIdentifier)
        );
    }
}
