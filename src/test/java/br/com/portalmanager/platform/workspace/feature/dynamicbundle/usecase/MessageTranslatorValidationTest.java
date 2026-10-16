package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.validation.messagetranslator.MessageTranslatorValidator;
import org.junit.jupiter.api.Test;

class MessageTranslatorValidationTest {

    private final MessageTranslatorRepository repository = mock(MessageTranslatorRepository.class);
    private final MessageTranslatorValidator validator = new MessageTranslatorValidator(repository);

    @Test
    void rejectsNullInputBeforeCheckingTranslatorRepository() {
        assertThatThrownBy(() -> validator.validateForCreate(null, null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnpagedRequests() {
        assertThatThrownBy(() -> validator.validatePagination(org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(ValidationException.class);
    }
}
