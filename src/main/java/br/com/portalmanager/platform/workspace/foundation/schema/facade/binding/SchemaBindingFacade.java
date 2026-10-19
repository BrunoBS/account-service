package br.com.portalmanager.platform.workspace.foundation.schema.facade.binding;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.*;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.binding.SchemaBindingService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchemaBindingFacade {

    private final SchemaBindingService service;

    public SchemaBindingFacade(SchemaBindingService s) {
        service = s;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public SchemaBindingOutput create(AuthorizationContext context, CreateSchemaBindingInput i) {
        return service.create(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<SchemaBindingOutput> findAll(AuthorizationContext context, String lifecycle) {
        return service.findAll(lifecycle);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public SchemaBindingOutput find(AuthorizationContext context, String i) {
        return service.findByIdentifier(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public SchemaBindingOutput update(AuthorizationContext context, String i, UpdateSchemaBindingInput in) {
        return service.update(i, in);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public SchemaBindingOutput activate(AuthorizationContext context, String i) {
        return service.activate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public SchemaBindingOutput inactivate(AuthorizationContext context, String i) {
        return service.inactivate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String i) {
        service.delete(i);
    }
}
