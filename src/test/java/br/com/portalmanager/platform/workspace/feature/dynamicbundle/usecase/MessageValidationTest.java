package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.MessageDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslatorDetails;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorNormalizer;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.message.MessageValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

class MessageValidationTest {

    private final MessageRepository messages = mock(MessageRepository.class);
    private final MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
    private final MessageValidator validator =
            new MessageValidator(messages, microservices);
    private final MessageNormalizer normalizer = new MessageNormalizer(new MessageTranslatorNormalizer());
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void requiresInactiveMessageForDeletionAndKeepsInactiveDataUnchanged() {
        Message message = message();
        assertThatThrownBy(message::requireInactiveForDeletion)
                .isInstanceOf(ValidationException.class);
        message.inactivate(now);
        message.requireInactiveForDeletion();
        assertThatThrownBy(() -> message.inactivate(now)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                message.update(
                                        1L,
                                        new MessageDetails("changed", "MSG-102", 400, null),
                                        now))
                .isInstanceOf(ValidationException.class);
        assertThat(message.getMessageKey()).isEqualTo("some.key");
        assertThat(message.getLifecycle().value()).isEqualTo("INACTIVE");
        message.activate(now);
        assertThat(message.getLifecycle().value()).isEqualTo("ACTIVE");
    }

    @Test
    void preservesExistingTranslatorsButBlocksNewLinksToInactiveParents() {
        Message message = message();
        MessageTranslator translator =
                new MessageTranslator(
                        message,
                        new MessageTranslatorDetails("pt-BR", "Title", "Detail", "Suggestion"),
                        now);
        message.inactivate(now);
        assertThat(translator.getLifecycle().value()).isEqualTo("ACTIVE");
        assertThatThrownBy(
                        () ->
                                new MessageTranslator(
                                        message,
                                        new MessageTranslatorDetails(
                                                "en-US", "Title", "Detail", "Suggestion"),
                                        now))
                .isInstanceOf(ValidationException.class);
        translator.update(
                new MessageTranslatorDetails("pt-BR", "Edited", "Detail", "Suggestion"), now);
        translator.inactivate(now);
        assertThatThrownBy(() -> translator.activate(now)).isInstanceOf(ValidationException.class);
        translator.requireInactiveForDeletion();
    }

    @Test
    void requiresInactiveTranslatorForIsolatedDeletion() {
        MessageTranslator translator =
                new MessageTranslator(
                        message(),
                        new MessageTranslatorDetails("pt-BR", "Title", "Detail", "Suggestion"),
                        now);
        assertThatThrownBy(translator::requireInactiveForDeletion)
                .isInstanceOf(ValidationException.class);
        translator.inactivate(now);
        translator.requireInactiveForDeletion();
        assertThat(translator.getLifecycle().value()).isEqualTo("INACTIVE");
        assertThatThrownBy(
                        () ->
                                translator.update(
                                        new MessageTranslatorDetails(
                                                "pt-BR", "Changed", "Detail", "Suggestion"),
                                        now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsStaleVersion() {
        assertThatThrownBy(() -> validator.validateVersion(2L, 1L))
                .isInstanceOf(ResourceVersionConflictException.class);
    }

    @Test
    void rejectsNullInputAndCreateVersionBeforeQueryingDatabase() {
        assertThatThrownBy(() -> validator.validateForCreate(null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validateForCreate(
                                        new CreateMessageInput(
                                                1L, "id", "key", "MSG-101", 400, null, null)))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(messages, microservices);
    }

    @Test
    void normalizesScriptsAndRejectsMalformedLocaleInsteadOfTruncatingIt() {
        MessageTranslatorNormalizer translatorNormalizer = new MessageTranslatorNormalizer();
        assertThat(translatorNormalizer.normalizeLocaleFilter(" zh_hant_tw ")).isEqualTo("zh-Hant-TW");
        assertThat(translatorNormalizer.normalizeLocaleFilter("pt-br")).isEqualTo("pt-BR");
        assertThatThrownBy(() -> translatorNormalizer.normalizeLocaleFilter("pt-BR-!"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void preservesUnknownFieldsAndOriginalTypesBeforeSchemaValidation() {
        JsonNode payload =
                JsonMapper.builder()
                        .build()
                        .readTree(
                                """
                                {"code":" msg-101 ","httpStatus":"400","unexpected":true,
                                 "translators":[{"locale":"pt-br","title":123,"unknown":"value"},false]}
                                """);
        JsonNode normalized = normalizer.normalizePayload(payload);
        assertThat(normalized.get("code").asString()).isEqualTo("MSG-101");
        assertThat(normalized.get("httpStatus").isString()).isTrue();
        assertThat(normalized.has("unexpected")).isTrue();
        assertThat(normalized.at("/translators/0/locale").asString()).isEqualTo("pt-BR");
        assertThat(normalized.at("/translators/0/title").isNumber()).isTrue();
        assertThat(normalized.at("/translators/0/unknown").asString()).isEqualTo("value");
        assertThat(normalized.at("/translators/1").isBoolean()).isTrue();
        assertThat(payload.get("code").asString()).isEqualTo(" msg-101 ");
    }

    private Message message() {
        return new Message(1L, new MessageDetails("some.key", "MSG-101", 400, null), now);
    }
}
