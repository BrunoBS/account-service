package br.com.portalmanager.platform.workspace.entrypoint.web.support.schema;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Replays a body consumed by ResourceSchemaRequestBodyAdvice so the selected
 * HttpMessageConverter can deserialize the same bytes normally.
 */
final class CachedBodyHttpInputMessage implements HttpInputMessage {

    private final HttpHeaders headers;
    private final byte[] body;

    CachedBodyHttpInputMessage(HttpHeaders headers, byte[] body) {
        this.headers = headers;
        this.body = body;
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }
}
