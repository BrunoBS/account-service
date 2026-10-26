package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;
    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("UPDATE - atualiza dados da mensagem")
    void atualizaMensagem() {
        var created = messages.create(factory.valid(microservice, "message.before", "MESSAGE-BEFORE"))
                .expectCreated().extractBody();
        messages.update(created.identifier(), factory.update(created.version(), microservice, "message.after", "MESSAGE-AFTER"))
                .expectOk();
        messages.findByIdentifier(created.identifier()).expectOk().expect("messageKey", "message.after");
    }

    @Test @DisplayName("UPDATE - rejeita versão desatualizada")
    void rejeitaVersaoDesatualizada() {
        var created = messages.create(factory.valid(microservice, "message.stale", "MESSAGE-STALE"))
                .expectCreated().extractBody();
        messages.update(created.identifier(), factory.update(created.version() + 10, microservice, "message.stale", "MESSAGE-STALE"))
                .expectConflict();
    }
}
