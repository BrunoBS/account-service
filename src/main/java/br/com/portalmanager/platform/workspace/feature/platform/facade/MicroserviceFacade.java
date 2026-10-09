package br.com.portalmanager.platform.workspace.feature.platform.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.*;import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.*;import org.springframework.stereotype.Service;import java.util.List;
@Service public class MicroserviceFacade {private final MicroserviceCommandService commandService;private final MicroserviceQueryService queryService;public MicroserviceFacade(MicroserviceCommandService c,MicroserviceQueryService q){commandService=c;queryService=q;}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public MicroserviceOutput create(CreateMicroserviceInput i){return commandService.create(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public MicroserviceOutput findByIdentifier(String i){return queryService.findByIdentifier(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public List<MicroserviceOutput> findAll(){return queryService.findAll();}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public MicroserviceOutput update(String i,UpdateMicroserviceInput in){return commandService.update(i,in);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.ACTIVATE) public MicroserviceOutput activate(String i){return commandService.activate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public MicroserviceOutput inactivate(String i){return commandService.inactivate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(String i){commandService.delete(i);}}
