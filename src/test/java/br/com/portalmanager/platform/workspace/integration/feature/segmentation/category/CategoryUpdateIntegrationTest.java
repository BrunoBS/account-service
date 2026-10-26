package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private CategoryClient categories;
    @Autowired private CategoryScenario scenario;
    private final CategoryFactory factory = new CategoryFactory();
    private String segment;
    private String identifier;
    @BeforeEach void prepare() { segment = scenario.setup(); identifier = categories.create(segment, factory.valid()).expectCreated().extractBody().identifier(); }
    @Test @DisplayName("UPDATE - atualiza categoria mantendo código") void atualizaCategoria() {
        var current = categories.findByIdentifier(segment, identifier).expectOk().extractBody();
        categories.update(segment, identifier, factory.update(current.version(), current.code(), "Updated category")).expectOk();
        categories.findByIdentifier(segment, identifier).expectOk().expect("name", "Updated category");
    }
    @Test @DisplayName("UPDATE - rejeita versão desatualizada") void rejeitaVersaoDesatualizada() {
        var current = categories.findByIdentifier(segment, identifier).expectOk().extractBody();
        categories.update(segment, identifier, factory.update(current.version() + 10, current.code(), "Stale")).expectConflict();
    }
}
