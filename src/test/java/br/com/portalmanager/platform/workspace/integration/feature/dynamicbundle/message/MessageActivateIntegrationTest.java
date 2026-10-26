package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;
    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("LIFECYCLE - ativa mensagem inativa")
    void ativaMensagemInativa() {
        String identifier = messages.create(factory.valid(microservice, "message.activate", "MESSAGE-ACTIVATE"))
                .expectCreated().extractBody().identifier();
        messages.inactivate(identifier).expectOk();
        messages.activate(identifier).expectOk();
        messages.findByIdentifier(identifier).expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test @DisplayName("LIFECYCLE - retorna não encontrado ao ativar mensagem ausente")
    void ativaMensagemInexistente() { messages.activate("missing-message").expectNotFound(); }
}
