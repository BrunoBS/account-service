package br.com.portalmanager.platform.workspace.entrypoint.web.application;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;

import br.com.portalmanager.platform.workspace.core.application.facade.ApplicationFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications")
public class ApplicationController {
 private final ApplicationFacade facade; public ApplicationController(ApplicationFacade facade){this.facade=facade;}
 @PostMapping public ResponseEntity<ApplicationResponse> create(AuthorizationContext context, @PathVariable String workspaceIdentifier,@RequestBody JsonNode payload) throws tools.jackson.core.JacksonException{return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(facade.create(context, workspaceIdentifier,payload)));}
 @GetMapping("/{identifier}") public ApplicationResponse find(AuthorizationContext context, @PathVariable String workspaceIdentifier,@PathVariable String identifier){return ApplicationResponse.from(facade.find(context, workspaceIdentifier,identifier));}
 @GetMapping public List<ApplicationResponse> findAll(AuthorizationContext context, @PathVariable String workspaceIdentifier,@RequestParam(defaultValue="true") Boolean active,@RequestParam(required=false) String tagName){return facade.findAll(context, workspaceIdentifier,active,tagName).stream().map(ApplicationResponse::from).toList();}
 @GetMapping("/summary") public List<ApplicationSummaryResponse> summary(AuthorizationContext context, @PathVariable String workspaceIdentifier){return facade.summary(context, workspaceIdentifier).stream().map(ApplicationSummaryResponse::from).toList();}
 @PutMapping("/{identifier}") public ApplicationResponse update(AuthorizationContext context, @PathVariable String workspaceIdentifier,@PathVariable String identifier,@RequestBody JsonNode payload) throws tools.jackson.core.JacksonException{return ApplicationResponse.from(facade.update(context, workspaceIdentifier,identifier,payload));}
 @PostMapping("/{identifier}/inactivate") public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String workspaceIdentifier,@PathVariable String identifier){facade.inactivate(context, workspaceIdentifier,identifier);return ResponseEntity.noContent().build();}
 @PostMapping("/{identifier}/restore") public ApplicationResponse restore(AuthorizationContext context, @PathVariable String workspaceIdentifier,@PathVariable String identifier){return ApplicationResponse.from(facade.restore(context, workspaceIdentifier,identifier));}
 @DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String workspaceIdentifier,@PathVariable String identifier){facade.delete(context, workspaceIdentifier,identifier);return ResponseEntity.noContent().build();}
}
