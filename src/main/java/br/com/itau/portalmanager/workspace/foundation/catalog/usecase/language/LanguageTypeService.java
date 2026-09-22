package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.language;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.language.LanguageType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.language.LanguageTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.language.LanguageTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LanguageTypeService extends EnumCatalogService<LanguageType, LanguageTypeEnum> {

    public LanguageTypeService(
            LanguageTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                LanguageType.class,
                LanguageTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
