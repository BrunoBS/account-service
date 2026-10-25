package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

public abstract class CatalogClient extends BaseClient {
    private final String path;

    protected CatalogClient(PlatformRequestSpecificationFactory requests, String path) {
        super(requests, JsonMapper.builder().build());
        this.path = path;
    }

    public CatalogResponse create(CatalogDTO request) {
        return response(ownerRequest().body(json(request)).post(path).then(), CatalogResponse::new);
    }

    public CatalogResponse findAll() {
        return response(ownerRequest().get(path).then(), CatalogResponse::new);
    }

    public CatalogResponse findAll(Map<String, String> filters) {
        return response(ownerRequest().queryParams(filters).get(path).then(), CatalogResponse::new);
    }

    public CatalogResponse findByCode(String code) {
        return response(ownerRequest().get(path + "/{code}", code).then(), CatalogResponse::new);
    }

    public CatalogResponse update(String code, CatalogDTO request) {
        return response(ownerRequest().body(json(request)).put(path + "/{code}", code).then(), CatalogResponse::new);
    }

    public CatalogResponse delete(String code) {
        return response(ownerRequest().delete(path + "/{code}", code).then(), CatalogResponse::new);
    }

    public CatalogResponse restore(String code) {
        return response(ownerRequest().post(path + "/{code}/restore", code).then(), CatalogResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "catalog-it")
                .accept(ContentType.JSON);
    }
}
