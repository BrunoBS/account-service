package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

public record UpdateMessageTranslationInput(
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion
) {
}
