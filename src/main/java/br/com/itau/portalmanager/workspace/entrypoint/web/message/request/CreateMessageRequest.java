package br.com.itau.portalmanager.workspace.entrypoint.web.message.request;

import br.com.itau.portalmanager.workspace.feature.message.usecase.model.CreateMessageInput;

import java.util.List;

public record CreateMessageRequest(
        String service,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation,
        List<CreateMessageTranslationRequest> translations
) {
    public CreateMessageInput toInput() {
        return new CreateMessageInput(
                service,
                messageKey,
                code,
                httpStatus,
                observation,
                translations == null ? null : translations.stream()
                        .map(value -> value == null ? null : value.toInput())
                        .toList()
        );
    }
}
