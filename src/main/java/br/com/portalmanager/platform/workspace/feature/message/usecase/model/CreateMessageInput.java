package br.com.portalmanager.platform.workspace.feature.message.usecase.model;

import java.util.List;

public record CreateMessageInput(
    Long version,
    String microserviceIdentifier,
    String messageKey,
    String code,
    Integer httpStatus,
    String observation,
    List<CreateMessageTranslationInput> translations
) {}
