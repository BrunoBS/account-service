package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime;


import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.library.observability.logging.sanitizer.DefaultLogSanitizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

/** Async errors use the messaging catalog and the observability structured-error field. */
@Component
public class DynamicParameterErrorLogger {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicParameterErrorLogger.class);
    private final ApiMessageResolver messages;
    private final ObjectMapper mapper;

    public DynamicParameterErrorLogger(ApiMessageResolver messages, ObjectMapper mapper) {
        this.messages = messages;
        this.mapper = mapper;
    }

    public void log(
            String identifier,
            String fullKey,
            JsonNode publishedValue,
            RuntimeException exception) {
        String key =
                exception instanceof ApiException api
                        ? api.getMessageKey()
                        : PlatformMessageKeys.INTERNAL_SERVER_ERROR;
        var error = mapper.createObjectNode();
        error.put("key", key);
        error.put("timestamp", java.time.Instant.now().toString());
        error.put("correlationId", MDC.get("correlationId"));
        error.put("identifier", identifier);
        error.put("fullKey", fullKey);
        // Store the sanitized JSON as text so masking numeric identifiers cannot break
        // the structured error JSON (for example, an unquoted CPF becomes a mask).
        error.put(
                "publishedValue",
                publishedValue == null
                        ? null
                        : DefaultLogSanitizer.INSTANCE.sanitize(publishedValue.toString()));
        error.put("reason", exception.getClass().getSimpleName());
        try {
            var message = messages.resolve(key, Locale.forLanguageTag("pt-BR"));
            error.put("code", message.code());
            error.put("message", message.message());
            error.put("solution", message.solution());
        } catch (RuntimeException resolutionFailure) {
            // A messaging outage must not interrupt the remaining publication items.
            error.put("message", "ParameterKey processing failed; message resolution unavailable.");
        }
        String previous = MDC.get("platform.error");
        try {
            MDC.put("platform.error", DefaultLogSanitizer.INSTANCE.sanitize(error.toString()));
            LOG.error("ParameterKey processing failed");
        } finally {
            if (previous == null) MDC.remove("platform.error");
            else MDC.put("platform.error", previous);
        }
    }
}
