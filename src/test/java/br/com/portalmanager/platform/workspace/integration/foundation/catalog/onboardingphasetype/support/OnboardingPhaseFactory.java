package br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public final class OnboardingPhaseFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO workspaceRegistration() {
        return phase("WORKSPACE_REGISTRATION", "Cadastro do workspace",
                "Cadastro dos dados principais do workspace.", 1,
                "Revise os dados principais do workspace.");
    }

    public CatalogDTO workspaceFirstEnvironment() {
        return phase("WORKSPACE_FIRST_ENVIRONMENT", "Configuração do ambiente do workspace",
                "Configuração do primeiro ambiente do workspace.", 2,
                "Configure o primeiro ambiente do workspace.");
    }

    public CatalogDTO firstApplicationRegistration() {
        return phase("FIRST_APPLICATION_REGISTRATION", "Cadastro da primeira aplicação",
                "Cadastro da primeira aplicação vinculada ao workspace.", 3,
                "Cadastre a primeira aplicação vinculada ao workspace.");
    }

    public CatalogDTO applicationFirstEnvironment() {
        return phase("APPLICATION_FIRST_ENVIRONMENT", "Configuração do ambiente da aplicação",
                "Configuração do primeiro ambiente da aplicação.", 4,
                "Configure o ambiente padrão da aplicação.");
    }

    public List<CatalogDTO> all() {
        return List.of(workspaceRegistration(), workspaceFirstEnvironment(),
                firstApplicationRegistration(), applicationFirstEnvironment());
    }

    private CatalogDTO phase(String code, String label, String description, int sortOrder, String orientation) {
        var settings = JsonMapper.builder().build().createObjectNode().put("orientation", orientation);
        return new CatalogDTO(code, label, description, sortOrder, settings);
    }
}
