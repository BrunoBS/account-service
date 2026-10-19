package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator;

public record MessageTranslatorOutput(
        String identifier,
        String messageIdentifier,
        Long version,
        String locale,
        String title,
        String detail,
        String suggestion,
        String lifecycle) {}
