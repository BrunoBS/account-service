package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.sharestatus;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus.ShareStatusType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus.ShareStatusTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.sharestatus.ShareStatusTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShareStatusTypeService extends EnumCatalogService<ShareStatusType, ShareStatusTypeEnum> {

    public ShareStatusTypeService(
            ShareStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ShareStatusType.class,
                ShareStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
