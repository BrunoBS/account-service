package br.com.portalmanager.platform.workspace.core.workspace.facade;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class WorkspaceFacade {
 private final WorkspaceCommandService command; private final WorkspaceQueryService query;
 public WorkspaceFacade(WorkspaceCommandService c,WorkspaceQueryService q){command=c;query=q;}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.CREATE) public WorkspaceOutput create(CreateWorkspaceInput i){return command.create(i);}
 @AuthorizationRequired(level=AuthorizationLevel.DEV,action=AuthorizationAction.READ) public WorkspaceOutput findByIdentifier(String i){return query.findByIdentifier(i);}
 @AuthorizationRequired(level=AuthorizationLevel.OPEN,action=AuthorizationAction.READ) public List<WorkspaceOutput> findAll(Boolean a,String t,String tag){return query.findAll(new FindAllWorkspacesInput(a,t,tag));}
 @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.UPDATE) public WorkspaceOutput update(String i,UpdateWorkspaceInput in){return command.update(i,in);}
 @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DEACTIVATE) public void inactivate(String i){command.inactivate(i);}
 @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.RESTORE) public WorkspaceOutput restore(String i){return command.restore(i);}
 @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.DELETE) public void delete(String i){command.delete(i);}
}
