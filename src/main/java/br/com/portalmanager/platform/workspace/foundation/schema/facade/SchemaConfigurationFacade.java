package br.com.portalmanager.platform.workspace.foundation.schema.facade;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaConfigurationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import org.springframework.stereotype.Service;

@Service
public class SchemaConfigurationFacade {
    private final SchemaConfigurationService service;
    public SchemaConfigurationFacade(SchemaConfigurationService service) { this.service = service; }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ResponseEntity<SchemaConfigurationResponse> create(CreateSchemaConfigurationRequest request) {
        var input = request == null ? null : request.toInput();
        return ResponseEntity.status(HttpStatus.CREATED).body(SchemaConfigurationResponse.from(service.create(input)));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<SchemaConfigurationResponse> findAll() {
        return service.findAll().stream().map(SchemaConfigurationResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public SchemaConfigurationResponse find(String identifier) {
        return SchemaConfigurationResponse.from(service.findByIdentifier(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public SchemaConfigurationResponse update(String identifier, UpdateSchemaConfigurationRequest request) {
        var input = request == null ? null : request.toInput();
        return SchemaConfigurationResponse.from(service.update(identifier, input));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public SchemaConfigurationResponse activate(String identifier) {
        return SchemaConfigurationResponse.from(service.activate(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public SchemaConfigurationResponse inactivate(String identifier) {
        return SchemaConfigurationResponse.from(service.inactivate(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(String identifier) {
        service.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
