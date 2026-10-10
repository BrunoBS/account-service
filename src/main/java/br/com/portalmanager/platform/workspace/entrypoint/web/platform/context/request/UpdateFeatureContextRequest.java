package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import java.util.List;

public record UpdateFeatureContextRequest(String name, String description, List<String> featureIdentifiers) {
    public UpdateFeatureContextInput toInput() {
        return new UpdateFeatureContextInput(name, description, featureIdentifiers);
    }
}
