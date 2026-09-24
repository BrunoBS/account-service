package br.com.itau.portalmanager.workspace.feature.message.usecase.model;

public record CreateMessageTranslationInput(
        String locale,
        String title,
        String detail,
        String suggestion
) {
}
