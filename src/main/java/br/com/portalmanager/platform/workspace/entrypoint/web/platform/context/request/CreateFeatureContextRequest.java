package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import java.util.List;

public record CreateFeatureContextRequest(
    String code,
    String name,
    String description,
    List<String> featureIdentifiers
) {
    public CreateFeatureContextInput toInput() {
        return new CreateFeatureContextInput(code, name, description, featureIdentifiers);
    }
}
