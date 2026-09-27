package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CreateApplicationRequest(String name, String alias, String acronym, String applicationScope,
                                       String authorizerGroup, String settings,
                                       @JsonProperty("isDefault") Boolean defaultApplication, List<String> tags) {
    public CreateApplicationInput toInput() {
        return new CreateApplicationInput(name, alias, acronym, applicationScope, authorizerGroup, settings,
                Boolean.TRUE.equals(defaultApplication), tags);
    }
}
