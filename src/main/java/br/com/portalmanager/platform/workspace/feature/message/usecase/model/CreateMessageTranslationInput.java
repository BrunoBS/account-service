package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

public record CreateMessageTranslationInput(Long version, String locale, String title, String detail, String suggestion) {}
