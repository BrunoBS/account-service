package br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request;

import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;

public record CreatePublisherRequest(String code, String name, String description, String scope, Boolean deprecated) {
    public CreatePublisherInput toInput() { return new CreatePublisherInput(code, name, description, scope, deprecated); }
}
