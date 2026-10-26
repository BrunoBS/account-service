package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private CategoryClient categories;
    @Autowired private CategoryScenario scenario;
    private final CategoryFactory factory = new CategoryFactory();
    private String segment;
    @BeforeEach void prepare() { segment = scenario.setup(); }
    @Test @DisplayName("CREATE - cria categoria vinculada ao segmento") void criaCategoria() {
        categories.create(segment, factory.valid("premium-members")).expectCreated()
                .expect("code", "premium-members").expect("segmentIdentifier", segment);
    }
    @Test @DisplayName("VALIDATION - rejeita categoria sem código") void rejeitaCodigoAusente() {
        categories.create(segment, factory.withoutCode()).expectBadRequest();
    }
}
