package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;
    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("DELETE - remove mensagem e confirma que deixou de existir")
    void removeMensagem() {
        String identifier = messages.create(factory.valid(microservice, "message.delete", "MESSAGE-DELETE"))
                .expectCreated().extractBody().identifier();
        messages.inactivate(identifier).expectOk();
        messages.delete(identifier).expectNoContent();
        messages.findByIdentifier(identifier).expectNotFound();
    }

    @Test @DisplayName("DELETE - retorna não encontrado para mensagem ausente")
    void removeMensagemInexistente() { messages.delete("missing-message").expectNotFound(); }
}
