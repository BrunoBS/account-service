package br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.version;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.version.SchemaVersionOutput;
import org.springframework.stereotype.Component;

@Component
public class SchemaVersionMapper {

    public SchemaVersionOutput toOutput(SchemaVersion version) {
        return new SchemaVersionOutput(version.getIdentifier(), version.getSchemaVersion(), version.getVersionName(),
            version.getStatus().toString(), version.getDefinition());
    }
}
