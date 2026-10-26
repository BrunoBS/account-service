package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private CategoryClient categories;
    @Autowired private CategoryScenario scenario;
    private final CategoryFactory factory = new CategoryFactory();
    private String segment;
    @BeforeEach void prepare() { segment = scenario.setup(); }
    @Test @DisplayName("QUERY - lista categorias do segmento filtradas por lifecycle") void listaPorLifecycle() {
        categories.create(segment, factory.valid()).expectCreated();
        categories.findAll(segment, Map.of("lifecycle", "ACTIVE", "page", 0, "size", 10)).expectOk();
    }
}
