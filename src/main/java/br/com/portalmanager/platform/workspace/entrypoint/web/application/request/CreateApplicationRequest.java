package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateApplicationRequest(String name, String alias, String acronym, String applicationScope,
                                       @JsonSetter(nulls = Nulls.FAIL) String authorizerGroup, @JsonSetter(nulls = Nulls.FAIL) JsonNode settings, @JsonSetter(nulls = Nulls.FAIL) List<String> tags) {
    public CreateApplicationInput toInput() {
        return new CreateApplicationInput(name, alias, acronym, applicationScope, authorizerGroup,
                settings, tags);
    }
}
