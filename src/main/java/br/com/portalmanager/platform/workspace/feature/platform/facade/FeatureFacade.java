package br.com.portalmanager.platform.workspace.feature.platform.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.*;import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.*;import org.springframework.stereotype.Service;import java.util.List;
@Service public class FeatureFacade {private final FeatureCommandService command;private final FeatureQueryService query;public FeatureFacade(FeatureCommandService c,FeatureQueryService q){command=c;query=q;}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public FeatureOutput create(CreateFeatureInput i){return command.create(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public FeatureOutput findByIdentifier(String i){return query.findByIdentifier(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public List<FeatureOutput> findAll(String c){return c==null?query.findAll():query.findByContext(c);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public List<FeatureContextOutput> findContexts(String i){return query.findContexts(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public FeatureOutput update(String i,UpdateFeatureInput in){return command.update(i,in);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.ACTIVATE) public FeatureOutput activate(String i){return command.activate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public FeatureOutput inactivate(String i){return command.inactivate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public FeatureOutput associateContext(String i,String c){return command.associateContext(i,c);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public FeatureOutput removeContext(String i,String c){return command.removeContext(i,c);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(String i){command.delete(i);}}
