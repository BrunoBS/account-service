package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** HTTP client used by shared scenarios to seed catalog resources with no dedicated fixture client. */
@Component
public final class CatalogSetupClient extends BaseClient {

    public CatalogSetupClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public CatalogResponse create(String path, CatalogDTO request) {
        return response(ownerRequest().body(json(request)).post(path).then(), CatalogResponse::new);
    }

    public CatalogResponse findByCode(String path, String code) {
        return response(ownerRequest().get(path + "/{code}", code).then(), CatalogResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "catalog-setup-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
