package br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.domain.VisibilityTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.repository.VisibilityTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class VisibilityTypeService extends EnumCatalogService<VisibilityType, VisibilityTypeEnum> {

    public VisibilityTypeService(
            VisibilityTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                VisibilityType.class,
                VisibilityTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
