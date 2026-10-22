package br.com.portalmanager.platform.workspace.feature.parameter.repository;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;

import org.junit.jupiter.api.Test;

class ParameterKeyJsonConverterTest {
    private final ParameterKeyJsonConverter converter = new ParameterKeyJsonConverter();

    @Test
    void convertsJsonBetweenDatabaseAndDomainRepresentations() {
        JsonNode json = tools.jackson.databind.json.JsonMapper.builder().build().readTree("{\"x\":1}");

        assertThat(converter.convertToDatabaseColumn(json)).isEqualTo("{\"x\":1}");
        assertThat(converter.convertToEntityAttribute("{\"x\":1}")).isEqualTo(json);
    }

    @Test
    void invalidPersistedJsonUsesTheGenericMessagingErrorWithoutLeakingTheValue() {
        String invalidValue = "sensitive-persisted-value{";

        assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidValue))
                .isInstanceOf(ApiException.class)
                .satisfies(
                        exception -> {
                            ApiException apiException = (ApiException) exception;
                            assertThat(apiException.getMessageKey())
                                    .isEqualTo(PlatformMessageKeys.INTERNAL_SERVER_ERROR);
                            assertThat(apiException.getCause()).isNull();
                            assertThat(apiException.getMessage()).doesNotContain(invalidValue);
                        });
    }
}
