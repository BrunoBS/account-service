package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

public record CreateMessageTranslationInput(String locale, String title, String detail, String suggestion) {}
