package br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator;

public record MessageTranslatorDetails(
        String locale, String title, String detail, String suggestion) {}
