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
public class PlatformSchemaFacade {

    private final SchemaCommandService commandService;
    private final SchemaQueryService queryService;
    private final SchemaVersionCommandService versionCommandService;

    public PlatformSchemaFacade(
            SchemaCommandService commandService,
            SchemaQueryService queryService,
            SchemaVersionCommandService versionCommandService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.versionCommandService = versionCommandService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ResponseEntity<SchemaResponse> create(CreateSchemaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaResponse.from(commandService.createPlatform(request.toPlatformInput())));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<SchemaResponse> findAll() {
        return queryService.findPlatform().stream().map(SchemaResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public SchemaResponse findByIdentifier(String identifier) {
        return SchemaResponse.from(queryService.findPlatformByIdentifier(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public SchemaResponse update(
            String identifier,
            UpdateSchemaRequest request
    ) {
        return SchemaResponse.from(commandService.updatePlatform(identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public SchemaResponse activate(String identifier) {
        return SchemaResponse.from(commandService.activatePlatform(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public SchemaResponse inactivate(String identifier) {
        return SchemaResponse.from(commandService.inactivatePlatform(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> quarantine(String identifier) {
        commandService.quarantinePlatform(identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ResponseEntity<SchemaVersionResponse> createVersion(
            String identifier,
            CreateSchemaVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaVersionResponse.from(
                        versionCommandService.createPlatformDraft(identifier, request.toInput())
                ));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> deleteDraft(
            String identifier,
            String versionIdentifier
    ) {
        versionCommandService.deletePlatformDraft(identifier, versionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<SchemaVersionResponse> findVersions(String identifier) {
        return queryService.findPlatformVersions(identifier).stream()
                .map(SchemaVersionResponse::from)
                .toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public SchemaVersionResponse publish(
            String identifier,
            String versionIdentifier
    ) {
        return SchemaVersionResponse.from(
                versionCommandService.publishPlatform(identifier, versionIdentifier)
        );
    }
}
