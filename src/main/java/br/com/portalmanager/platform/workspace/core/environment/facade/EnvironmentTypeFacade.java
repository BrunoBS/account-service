package br.com.portalmanager.platform.workspace.core.environment.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.core.environment.usecase.model.*;import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.*;import org.springframework.stereotype.Service;import java.util.List;
@Service public class EnvironmentTypeFacade {private final EnvironmentTypeCommandService command;private final EnvironmentTypeQueryService query;public EnvironmentTypeFacade(EnvironmentTypeCommandService c,EnvironmentTypeQueryService q){command=c;query=q;}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public EnvironmentTypeOutput create(EnvironmentTypeInput i){return command.create(i);}
@AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public List<EnvironmentTypeOutput> list(){return query.list();}
@AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public EnvironmentTypeOutput find(String i){return query.findOutput(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public EnvironmentTypeOutput update(String i,EnvironmentTypeInput in){return command.update(i,in);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public void inactivate(String i){command.inactivate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.RESTORE) public EnvironmentTypeOutput restore(String i){return command.restore(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(String i){command.delete(i);}}
