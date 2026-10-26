package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageTranslatorInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageTranslatorClient translators;
    @Autowired private MessageTranslatorScenario scenario;
    private final MessageTranslatorFactory factory = new MessageTranslatorFactory();
    private String message;
    @BeforeEach void prepare() { message = scenario.setup(); }

    @Test @DisplayName("LIFECYCLE - inativa tradução ativa")
    void inativaTraducaoAtiva() {
        String identifier = translators.create(message, factory.valid("pt-BR")).expectCreated().extractBody().identifier();
        translators.inactivate(message, identifier).expectOk();
        translators.findByIdentifier(message, identifier).expectOk().expect("lifecycle", "INACTIVE");
    }
}
