package br.com.portalmanager.platform.workspace.foundation.schema.usecase.mapping.binding;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.binding.SchemaBinding;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.SchemaBindingOutput;
import org.springframework.stereotype.Component;

@Component
public class SchemaBindingMapper {

    public SchemaBindingOutput toOutput(SchemaBinding binding) {
        return new SchemaBindingOutput(binding.getIdentifier(), binding.getVersion(),
            binding.getResourceType(), binding.getResourceCode(),
            binding.getSchema().getIdentifier(), binding.getLifecycle().value());
    }
}
