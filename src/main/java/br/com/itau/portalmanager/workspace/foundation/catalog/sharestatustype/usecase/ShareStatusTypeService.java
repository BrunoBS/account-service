package br.com.itau.portalmanager.workspace.foundation.catalog.sharestatustype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import br.com.itau.portalmanager.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.sharestatustype.repository.ShareStatusTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShareStatusTypeService extends EnumCatalogService<ShareStatusType, ShareStatusTypeEnum> {

    public ShareStatusTypeService(
            ShareStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                ShareStatusType.class,
                ShareStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
