package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.mapping.messagetranslator;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslatorDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository.DeletionSnapshot;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import org.springframework.stereotype.Component;

@Component
public class MessageTranslatorMapper {

    public MessageTranslatorDetails toDetails(CreateMessageTranslatorInput input) {
        return new MessageTranslatorDetails(
                input.locale(), input.title(), input.detail(), input.suggestion());
    }

    public MessageTranslator toDomain(
            Message message, CreateMessageTranslatorInput input, java.time.LocalDateTime now) {
        return new MessageTranslator(message, toDetails(input), now);
    }

    public MessageTranslatorDetails toDetails(UpdateMessageTranslatorInput input) {
        return new MessageTranslatorDetails(
                input.locale(), input.title(), input.detail(), input.suggestion());
    }

    public MessageTranslatorOutput toOutput(MessageTranslator translator) {
        return new MessageTranslatorOutput(
                translator.getIdentifier(), translator.getMessage().getIdentifier(),
                translator.getVersion(), translator.getLocale(), translator.getTitle(),
                translator.getDetail(), translator.getSuggestion(), translator.getLifecycle().value());
    }

    public MessageTranslatorOutput toOutput(Message message, DeletionSnapshot snapshot) {
        return new MessageTranslatorOutput(
                snapshot.getIdentifier(), message.getIdentifier(), snapshot.getVersion(),
                snapshot.getLocale(), snapshot.getTitle(), snapshot.getDetail(),
                snapshot.getSuggestion(), snapshot.getLifecycle());
    }
}
