package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterErrorLogger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import tools.jackson.databind.json.JsonMapper;

class DynamicParameterErrorLoggerTest {
    @Test
    void masksPublishedValuesAndPreservesStructuredJsonAndThePreviousErrorContext() {
        var mapper = JsonMapper.builder().build();
        var messages = mock(ApiMessageResolver.class);
        when(messages.resolve(anyString(), any()))
                .thenReturn(new ApiMessage("CODE", "key", "pt-BR", "Failure", "Retry", 500));
        var errors = new DynamicParameterErrorLogger(messages, mapper);
        Logger logger = (Logger) LoggerFactory.getLogger(DynamicParameterErrorLogger.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        String previous = MDC.get("platform.error");
        MDC.put("platform.error", "previous");
        try {
            errors.log(
                    "identifier",
                    "service.key",
                    mapper.readTree("{\"password\":\"secret-value\",\"cpf\":12345678901}"),
                    new IllegalStateException("secret-value"));
            assertThat(events.list).hasSize(1);
            var event = events.list.getFirst();
            String json = event.getMDCPropertyMap().get("platform.error");
            assertThat(json).doesNotContain("secret-value", "12345678901");
            var error = mapper.readTree(json);
            assertThat(error.get("identifier").asString()).isEqualTo("identifier");
            assertThat(error.get("fullKey").asString()).isEqualTo("service.key");
            assertThat(error.get("publishedValue").asString()).contains("***");
            assertThat(MDC.get("platform.error")).isEqualTo("previous");
            assertThat(event.getThrowableProxy()).isNull();
        } finally {
            if (previous == null) MDC.remove("platform.error");
            else MDC.put("platform.error", previous);
            logger.detachAppender(events);
            events.stop();
        }
    }
}
