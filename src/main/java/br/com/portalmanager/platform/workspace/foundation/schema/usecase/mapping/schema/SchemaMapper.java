package br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.schema;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.SchemaOutput;
import org.springframework.stereotype.Component;

@Component
public class SchemaMapper {

    public SchemaOutput toOutput(Schema schema, String workspaceIdentifier) {
        return new SchemaOutput(schema.getIdentifier(), schema.getVersion(), schema.getScope().toString(),
            workspaceIdentifier, schema.getCode(), schema.getName(), schema.getDescription(),
            schema.getLifecycle().toString());
    }
}
