package br.com.portalmanager.platform.workspace.core.environment.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.core.environment.usecase.model.*;import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.*;import org.springframework.stereotype.Service;import java.util.List;
@Service public class WorkspaceEnvironmentFacade {private final EnvironmentCommandService command;private final EnvironmentQueryService query;public WorkspaceEnvironmentFacade(EnvironmentCommandService c,EnvironmentQueryService q){command=c;query=q;}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.CREATE) public EnvironmentOutput create(String w,CreateEnvironmentInput i){return command.createCustom(w,i);}
@AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public List<EnvironmentOutput> list(String w,Boolean a){return query.listCustom(w,a);}
@AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public EnvironmentOutput find(String w,String i){return query.findCustom(w,i);}
@AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public List<EnvironmentOutput> roots(String w){return query.roots(w);}
@AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public List<EnvironmentOutput> children(String w,String i){return query.children(w,i);}
@AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public List<EnvironmentTreeOutput> tree(String w){return query.tree(w);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.UPDATE) public EnvironmentOutput update(String w,String i,UpdateEnvironmentInput in){return command.updateCustom(w,i,in);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DEACTIVATE) public void inactivate(String w,String i){command.inactivateCustom(w,i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.RESTORE) public EnvironmentOutput restore(String w,String i){return command.restoreCustom(w,i);}
@AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DELETE) public void delete(String w,String i){command.deleteCustom(w,i);}}
