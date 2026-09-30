package br.com.portalmanager.platform.workspace.entrypoint.web.application;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationCommandService;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.request.CreateApplicationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.request.UpdateApplicationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationSummaryResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.ResourceSchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications")
public class ApplicationController {
    private static final String RESOURCE_TYPE = "APPLICATION";
    private static final String RESOURCE_CODE = "application";

    private final ApplicationCommandService command;
    private final ApplicationQueryService query;
    private final ResourceSchemaValidator resourceSchemaValidator;
    private final ObjectMapper objectMapper;

    public ApplicationController(ApplicationCommandService command,
                                 ApplicationQueryService query,
                                 ResourceSchemaValidator resourceSchemaValidator,
                                 ObjectMapper objectMapper) {
        this.command = command;
        this.query = query;
        this.resourceSchemaValidator = resourceSchemaValidator;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "INSERT", resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier"))
    public ResponseEntity<ApplicationResponse> create(@PathVariable String workspaceIdentifier,
                                                       @RequestBody JsonNode payload) {
        resourceSchemaValidator.validate(RESOURCE_TYPE, RESOURCE_CODE, payload);
        CreateApplicationRequest request = objectMapper.treeToValue(payload, CreateApplicationRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApplicationResponse.from(command.create(workspaceIdentifier, request.toInput())));
    }

    @GetMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public ApplicationResponse find(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return ApplicationResponse.from(query.findByIdentifier(workspaceIdentifier, identifier));
    }

    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<ApplicationResponse> findAll(@PathVariable String workspaceIdentifier,
                                              @RequestParam(defaultValue = "true") Boolean active,
                                              @RequestParam(required = false) String tagName) {
        return query.findAll(workspaceIdentifier, active, tagName).stream().map(ApplicationResponse::from).toList();
    }

    @GetMapping("/summary")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<ApplicationSummaryResponse> summary(@PathVariable String workspaceIdentifier) {
        return query.summary(workspaceIdentifier).stream().map(ApplicationSummaryResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    @Auditable(resource = "APPLICATION", action = "UPDATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ApplicationResponse update(@PathVariable String workspaceIdentifier,
                                      @PathVariable String identifier,
                                      @RequestBody JsonNode payload) {
        resourceSchemaValidator.validate(RESOURCE_TYPE, RESOURCE_CODE, payload);
        UpdateApplicationRequest request = objectMapper.treeToValue(payload, UpdateApplicationRequest.class);
        return ApplicationResponse.from(command.update(workspaceIdentifier, identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "INACTIVATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ResponseEntity<Void> inactivate(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        command.inactivate(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "RESTORE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ApplicationResponse restore(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return ApplicationResponse.from(command.restore(workspaceIdentifier, identifier));
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "DELETE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ResponseEntity<Void> delete(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        command.delete(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
