package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Creates a message through HTTP as the parent resource for translator scenarios. */
@Component
public final class MessageTranslatorScenario implements TestScenario<String> {
    private final MessageScenario messagesScenario;
    private final MessageClient messages;

    public MessageTranslatorScenario(MessageScenario messagesScenario, MessageClient messages) {
        this.messagesScenario = Objects.requireNonNull(messagesScenario);
        this.messages = Objects.requireNonNull(messages);
    }

    @Override public String setup() {
        String microservice = messagesScenario.setup();
        return messages.create(new MessageFactory().valid(microservice, "translator.parent", "TRANSLATOR-PARENT"))
                .expectCreated().extractBody().identifier();
    }
}
