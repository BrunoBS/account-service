package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.PublicationModeInput;

public record PublicationModeRequest(String publicationModeCode) {
    public PublicationModeInput toInput() { return new PublicationModeInput(publicationModeCode); }
}
