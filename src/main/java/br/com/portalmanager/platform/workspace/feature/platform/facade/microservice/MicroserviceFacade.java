package br.com.portalmanager.platform.workspace.feature.platform.facade.microservice;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.UpdateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response.MicroserviceResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MicroserviceFacade {
 private final MicroserviceCommandService commandService;
 private final MicroserviceQueryService queryService;
 public MicroserviceFacade(MicroserviceCommandService commandService, MicroserviceQueryService queryService) {
 this.commandService = commandService; this.queryService = queryService;
 }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
 public MicroserviceResponse create(CreateMicroserviceRequest request) { return MicroserviceResponse.from(commandService.create(request.toInput())); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
 public MicroserviceResponse findByIdentifier(String identifier) { return MicroserviceResponse.from(queryService.findByIdentifier(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
 public List<MicroserviceResponse> findAll() { return queryService.findAll().stream().map(MicroserviceResponse::from).toList(); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
 public MicroserviceResponse update(String identifier, UpdateMicroserviceRequest request) { return MicroserviceResponse.from(commandService.update(identifier, request.toInput())); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
 public MicroserviceResponse activate(String identifier) { return MicroserviceResponse.from(commandService.activate(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
 public MicroserviceResponse inactivate(String identifier) { return MicroserviceResponse.from(commandService.inactivate(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
 public void delete(String identifier) { commandService.delete(identifier); }
}
