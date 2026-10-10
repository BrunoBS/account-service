package br.com.portalmanager.platform.workspace.feature.message.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import br.com.portalmanager.platform.workspace.feature.message.usecase.validation.MessageValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MessageValidationTest {

    private final MessageValidator validator = new MessageValidator(null);

    @Test
    void rejectsDeletionUntilMessageIsInactive() {
        Message message = new Message(1L, "some.key", "MSG-101", 400, null, LocalDateTime.now());
        assertThatThrownBy(() -> validator.validateDeletion(message)).isInstanceOf(ValidationException.class);
        message.inactivate(LocalDateTime.now());
        validator.validateDeletion(message);
    }

    @Test
    void rejectsStaleVersion() {
        assertThatThrownBy(() -> validator.validateVersion(2L, 1L)).isInstanceOf(
            ResourceVersionConflictException.class
        );
    }
}
