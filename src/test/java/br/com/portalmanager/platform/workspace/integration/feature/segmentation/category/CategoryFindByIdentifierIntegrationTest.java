package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private CategoryClient categories;
    @Autowired private CategoryScenario scenario;
    private final CategoryFactory factory = new CategoryFactory();
    private String segment;
    private String identifier;
    @BeforeEach void prepare() { segment = scenario.setup(); identifier = categories.create(segment, factory.valid()).expectCreated().extractBody().identifier(); }
    @Test @DisplayName("QUERY - busca categoria dentro do segmento") void encontraCategoria() {
        categories.findByIdentifier(segment, identifier).expectOk().expect("identifier", identifier);
    }
    @Test @DisplayName("QUERY - retorna não encontrado para categoria ausente") void categoriaAusente() {
        categories.findByIdentifier(segment, "missing-category").expectNotFound();
    }
}
