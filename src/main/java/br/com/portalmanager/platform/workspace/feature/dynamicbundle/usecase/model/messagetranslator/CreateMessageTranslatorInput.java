package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator;

public record CreateMessageTranslatorInput(
        Long version, String locale, String title, String detail, String suggestion) {}
