package br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.domain.LanguageType;
import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.domain.LanguageTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.repository.LanguageTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LanguageTypeService extends EnumCatalogService<LanguageType, LanguageTypeEnum> {

    public LanguageTypeService(
            LanguageTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                LanguageType.class,
                LanguageTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
