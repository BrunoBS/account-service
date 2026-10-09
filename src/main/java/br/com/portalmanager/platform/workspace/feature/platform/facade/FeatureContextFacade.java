package br.com.portalmanager.platform.workspace.feature.platform.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.CreateFeatureContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.UpdateFeatureContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.FeatureContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.FeatureContextQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FeatureContextFacade {
 private final FeatureContextCommandService commandService;
 private final FeatureContextQueryService queryService;
 public FeatureContextFacade(FeatureContextCommandService commandService, FeatureContextQueryService queryService) {
 this.commandService = commandService; this.queryService = queryService;
 }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
 public FeatureContextResponse create(CreateFeatureContextRequest request) { return FeatureContextResponse.from(commandService.create(request.toInput())); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
 public FeatureContextResponse findByIdentifier(String identifier) { return FeatureContextResponse.from(queryService.findByIdentifier(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
 public List<FeatureContextResponse> findAll() { return queryService.findAll().stream().map(FeatureContextResponse::from).toList(); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
 public FeatureContextResponse update(String identifier, UpdateFeatureContextRequest request) { return FeatureContextResponse.from(commandService.update(identifier, request.toInput())); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
 public FeatureContextResponse activate(String identifier) { return FeatureContextResponse.from(commandService.activate(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
 public FeatureContextResponse inactivate(String identifier) { return FeatureContextResponse.from(commandService.inactivate(identifier)); }
 @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
 public void delete(String identifier) { commandService.delete(identifier); }
}
