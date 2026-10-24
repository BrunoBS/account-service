package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.json.JsonMapper;

final class EnvironmentConfigurationClient extends BaseClient {

    EnvironmentConfigurationClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
        super(requests, jsonMapper);
    }

    RequestSpecification protocolRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("configuration-it").build())
                .header("correlation-id", "configuration-it")
                .accept(ContentType.JSON);
    }

    EnvironmentConfigurationResponse create(String path, Object body) {
        RequestSpecification request = protocolRequest();
        if (body != null) {
            request.body(body);
        }
        return response(request.post(path).then(), EnvironmentConfigurationResponse::new);
    }

    EnvironmentConfigurationResponse update(String path, Object body) {
        return response(protocolRequest().body(body).put(path).then(), EnvironmentConfigurationResponse::new);
    }

    EnvironmentConfigurationResponse find(String path) {
        return response(protocolRequest().get(path).then(), EnvironmentConfigurationResponse::new);
    }
}
