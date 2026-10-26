package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;
    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("QUERY - busca mensagem pelo identificador")
    void encontraMensagemCriada() {
        String identifier = messages.create(factory.valid(microservice, "message.find", "MESSAGE-FIND"))
                .expectCreated().extractBody().identifier();
        var found = messages.findByIdentifier(identifier).expectOk().extractBody();
        messages.findByIdentifier(identifier).expectOk()
                .expect("identifier", identifier)
                .expect("microserviceIdentifier", microservice);
    }

    @Test @DisplayName("QUERY - retorna não encontrado para identificador inexistente")
    void retornaNaoEncontradoParaMensagemAusente() {
        messages.findByIdentifier("missing-message").expectNotFound();
    }
}
