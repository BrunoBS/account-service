package br.com.portalmanager.platform.workspace.foundation.schema.facade;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;import br.com.portalmanager.platform.library.authorization.model.*;import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.*;import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;import org.springframework.stereotype.Service;import java.util.List;
@Service public class SchemaConfigurationFacade {private final SchemaConfigurationService service;public SchemaConfigurationFacade(SchemaConfigurationService s){service=s;}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.CREATE) public SchemaConfigurationOutput create(AuthorizationContext context, CreateSchemaConfigurationInput i){return service.create(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public List<SchemaConfigurationOutput> findAll(AuthorizationContext context){return service.findAll();}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.READ) public SchemaConfigurationOutput find(AuthorizationContext context, String i){return service.findByIdentifier(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.UPDATE) public SchemaConfigurationOutput update(AuthorizationContext context, String i,UpdateSchemaConfigurationInput in){return service.update(i,in);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.ACTIVATE) public SchemaConfigurationOutput activate(AuthorizationContext context, String i){return service.activate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DEACTIVATE) public SchemaConfigurationOutput inactivate(AuthorizationContext context, String i){return service.inactivate(i);}
@AuthorizationRequired(level=AuthorizationLevel.OWNER,action=AuthorizationAction.DELETE) public void delete(AuthorizationContext context, String i){service.delete(i);}}
