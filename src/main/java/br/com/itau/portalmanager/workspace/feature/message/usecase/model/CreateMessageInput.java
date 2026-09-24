package br.com.itau.portalmanager.workspace.feature.message.usecase.model;

import java.util.List;

public record CreateMessageInput(
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation,
        List<CreateMessageTranslationInput> translations
) {
}
