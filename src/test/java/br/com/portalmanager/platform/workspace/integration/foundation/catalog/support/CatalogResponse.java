package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import java.util.List;

public final class CatalogResponse extends BaseResponse<CatalogResponse> {
    public CatalogResponse(ValidatableResponse response) { super(response); }
    public CatalogDTO extractBody() { return extract(CatalogDTO.class); }
    public List<CatalogDTO> extractList() {
        return response.extract().jsonPath().getList("", CatalogDTO.class);
    }
    public int statusCode() { return response.extract().statusCode(); }
}
