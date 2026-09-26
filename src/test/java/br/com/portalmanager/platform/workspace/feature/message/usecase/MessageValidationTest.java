package br.com.portalmanager.platform.workspace.feature.message.usecase;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageValidationTest {
    private final MessageValidator validator = new MessageValidator(null);

    @Test
    void rejectsDeletionUntilMessageIsInactive() {
        Message message = new Message(1L, "some.key", "MSG-101", 400, null, LocalDateTime.now());
        assertThatThrownBy(() -> validator.validateDeletion(message))
                .isInstanceOf(ValidationException.class);
        message.inactivate(LocalDateTime.now());
        validator.validateDeletion(message);
    }

    @Test
    void rejectsStaleVersion() {
        assertThatThrownBy(() -> validator.validateVersion(2L, 1L))
                .isInstanceOf(ResourceVersionConflictException.class);
    }
}
