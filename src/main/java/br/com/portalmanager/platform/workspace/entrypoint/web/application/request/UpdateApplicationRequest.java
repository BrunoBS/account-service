package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UpdateApplicationRequest(Long version, String name, String alias, String acronym, String applicationScope,
                                       String authorizerGroup, String settings,
                                       @JsonProperty("isDefault") Boolean defaultApplication, List<String> tags) {
    public UpdateApplicationInput toInput() {
        return new UpdateApplicationInput(version, name, alias, acronym, applicationScope, authorizerGroup, settings,
                Boolean.TRUE.equals(defaultApplication), tags);
    }
}
