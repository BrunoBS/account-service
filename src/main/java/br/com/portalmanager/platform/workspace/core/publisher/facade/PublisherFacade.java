package br.com.portalmanager.platform.workspace.core.publisher.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class PublisherFacade {
 private final PublisherCommandService command; private final PublisherQueryService query;
 public PublisherFacade(PublisherCommandService c,PublisherQueryService q){command=c;query=q;}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public PublisherOutput create(CreatePublisherInput i){return command.create(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public List<PublisherOutput> list(Boolean a,String s){return query.list(a,s);}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public PublisherOutput find(String i){return query.find(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public PublisherOutput update(String i,UpdatePublisherInput in){return command.update(i,in);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public void inactivate(String i){command.inactivate(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.RESTORE) public PublisherOutput restore(String i){return command.restore(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(String i){command.delete(i);}
}
