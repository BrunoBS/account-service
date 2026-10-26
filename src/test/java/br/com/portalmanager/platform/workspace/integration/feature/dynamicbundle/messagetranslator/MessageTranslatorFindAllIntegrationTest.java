package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageTranslatorFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageTranslatorClient translators;
    @Autowired private MessageTranslatorScenario scenario;
    private final MessageTranslatorFactory factory = new MessageTranslatorFactory();
    private String message;
    @BeforeEach void prepare() { message = scenario.setup(); }

    @Test @DisplayName("QUERY - lista traduções filtradas por locale")
    void listaPorLocale() {
        translators.create(message, factory.valid("pt-BR")).expectCreated();
        translators.create(message, factory.valid("en-US")).expectCreated();
        translators.findAll(message, Map.of("locale", "pt-BR")).expectOk();
    }
}
