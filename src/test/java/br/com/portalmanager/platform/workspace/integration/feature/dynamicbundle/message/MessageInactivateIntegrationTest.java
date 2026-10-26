package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;
    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("LIFECYCLE - inativa mensagem ativa")
    void inativaMensagemAtiva() {
        String identifier = messages.create(factory.valid(microservice, "message.inactivate", "MESSAGE-INACTIVATE"))
                .expectCreated().extractBody().identifier();
        messages.inactivate(identifier).expectOk();
        messages.findByIdentifier(identifier).expectOk().expect("lifecycle", "INACTIVE");
    }
}
