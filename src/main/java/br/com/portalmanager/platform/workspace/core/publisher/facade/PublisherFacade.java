package br.com.portalmanager.platform.workspace.core.publisher.facade;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class PublisherFacade {
 private final PublisherCommandService command; private final PublisherQueryService query;
 public PublisherFacade(PublisherCommandService c,PublisherQueryService q){command=c;query=q;}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public PublisherOutput create(AuthorizationContext context, CreatePublisherInput i){return command.create(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public List<PublisherOutput> list(AuthorizationContext context, Boolean a,String s){return query.list(a,s);}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public PublisherOutput find(AuthorizationContext context, String i){return query.find(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public PublisherOutput update(AuthorizationContext context, String i,UpdatePublisherInput in){return command.update(i,in);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public void inactivate(AuthorizationContext context, String i){command.inactivate(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.RESTORE) public PublisherOutput restore(AuthorizationContext context, String i){return command.restore(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(AuthorizationContext context, String i){command.delete(i);}
}
