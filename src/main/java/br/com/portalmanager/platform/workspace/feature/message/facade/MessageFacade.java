package br.com.portalmanager.platform.workspace.feature.message.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.feature.message.usecase.model.*;import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.*;import org.springframework.stereotype.Service;import java.util.List;
@Service public class MessageFacade {private final MessageCommandService commandService;private final MessageQueryService queryService;public MessageFacade(MessageCommandService c,MessageQueryService q){commandService=c;queryService=q;}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.CREATE) public MessageOutput create(CreateMessageInput i){return commandService.create(i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.READ) public MessageOutput findByIdentifier(String i){return queryService.findByIdentifier(i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.READ) public List<MessageOutput> findAll(String m,Boolean a,String c,String k){return queryService.findAll(m,a,c,k);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.UPDATE) public MessageOutput update(String i,UpdateMessageInput in){return commandService.update(i,in);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.ACTIVATE) public MessageOutput activate(String i){return commandService.activate(i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DEACTIVATE) public MessageOutput inactivate(String i){return commandService.inactivate(i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DELETE) public void delete(String i){commandService.delete(i);}}
