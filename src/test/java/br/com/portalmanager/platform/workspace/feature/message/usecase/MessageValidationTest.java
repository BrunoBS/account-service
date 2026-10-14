package br.com.portalmanager.platform.workspace.feature.message.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageDetails;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslationDetails;
import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.message.repository.MessageTranslationRepository;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MessageValidationTest {

    private final MessageRepository messages = mock(MessageRepository.class);
    private final MessageTranslationRepository translations = mock(MessageTranslationRepository.class);
    private final MicroserviceQueryService microservices = mock(MicroserviceQueryService.class);
    private final MessageValidator validator = new MessageValidator(messages, translations, microservices);
    private final MessageNormalizer normalizer = new MessageNormalizer();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void requiresInactiveMessageForDeletionAndKeepsInactiveDataUnchanged() {
        Message message = message();
        assertThatThrownBy(message::requireInactiveForDeletion).isInstanceOf(ValidationException.class);
        message.inactivate(now);
        message.requireInactiveForDeletion();
        assertThatThrownBy(() -> message.inactivate(now)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> message.update(1L, new MessageDetails("changed", "MSG-102", 400, null), now)).isInstanceOf(ValidationException.class);
        assertThat(message.getMessageKey()).isEqualTo("some.key");
        assertThat(message.getLifecycle().value()).isEqualTo("INACTIVE");
        message.activate(now);
        assertThat(message.getLifecycle().value()).isEqualTo("ACTIVE");
    }

    @Test
    void preservesExistingTranslationsButBlocksNewLinksToInactiveParents() {
        Message message = message();
        MessageTranslation translation = new MessageTranslation(message, new MessageTranslationDetails("pt-BR", "Title", "Detail", "Suggestion"), now);
        message.inactivate(now);
        assertThat(translation.getLifecycle().value()).isEqualTo("ACTIVE");
        assertThatThrownBy(() -> new MessageTranslation(message, new MessageTranslationDetails("en-US", "Title", "Detail", "Suggestion"), now)).isInstanceOf(ValidationException.class);
        translation.update(new MessageTranslationDetails("pt-BR", "Edited", "Detail", "Suggestion"), now);
        translation.inactivate(now);
        assertThatThrownBy(() -> translation.activate(now)).isInstanceOf(ValidationException.class);
        translation.requireInactiveForDeletion();
    }

    @Test
    void requiresInactiveTranslationForIsolatedDeletion() {
        MessageTranslation translation = new MessageTranslation(message(), new MessageTranslationDetails("pt-BR", "Title", "Detail", "Suggestion"), now);
        assertThatThrownBy(translation::requireInactiveForDeletion).isInstanceOf(ValidationException.class);
        translation.inactivate(now);
        translation.requireInactiveForDeletion();
        assertThat(translation.getLifecycle().value()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> translation.update(new MessageTranslationDetails("pt-BR", "Changed", "Detail", "Suggestion"), now)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsStaleVersion() {
        assertThatThrownBy(() -> validator.validateVersion(2L, 1L)).isInstanceOf(ResourceVersionConflictException.class);
    }

    @Test
    void rejectsNullInputAndCreateVersionBeforeQueryingDatabase() {
        assertThatThrownBy(() -> validator.validateForCreate(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateForCreate(new CreateMessageInput(1L, "id", "key", "MSG-101", 400, null, null))).isInstanceOf(ValidationException.class);
        verifyNoInteractions(messages, translations, microservices);
    }

    @Test
    void normalizesScriptsAndRejectsMalformedLocaleInsteadOfTruncatingIt() {
        assertThat(normalizer.normalizeLocaleFilter(" zh_hant_tw ")).isEqualTo("zh-Hant-TW");
        assertThat(normalizer.normalizeLocaleFilter("pt-br")).isEqualTo("pt-BR");
        assertThatThrownBy(() -> normalizer.normalizeLocaleFilter("pt-BR-!")).isInstanceOf(ValidationException.class);
    }

    @Test
    void preservesUnknownFieldsAndOriginalTypesBeforeSchemaValidation() {
        JsonNode payload = JsonMapper.builder().build().readTree("""
            {"code":" msg-101 ","httpStatus":"400","unexpected":true,
             "translations":[{"locale":"pt-br","title":123,"unknown":"value"},false]}
            """);
        JsonNode normalized = normalizer.normalizePayload(payload);
        assertThat(normalized.get("code").asString()).isEqualTo("MSG-101");
        assertThat(normalized.get("httpStatus").isString()).isTrue();
        assertThat(normalized.has("unexpected")).isTrue();
        assertThat(normalized.at("/translations/0/locale").asString()).isEqualTo("pt-BR");
        assertThat(normalized.at("/translations/0/title").isNumber()).isTrue();
        assertThat(normalized.at("/translations/0/unknown").asString()).isEqualTo("value");
        assertThat(normalized.at("/translations/1").isBoolean()).isTrue();
        assertThat(payload.get("code").asString()).isEqualTo(" msg-101 ");
    }

    private Message message() {
        return new Message(1L, new MessageDetails("some.key", "MSG-101", 400, null), now);
    }
}
