package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;

import java.util.List;

public record CreateMessageInput(
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation,
        List<CreateMessageTranslatorInput> translators) {}
