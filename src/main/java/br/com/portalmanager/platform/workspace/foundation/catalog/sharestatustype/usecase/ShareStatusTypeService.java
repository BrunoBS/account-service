package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.repository.ShareStatusTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShareStatusTypeService extends EnumCatalogService<ShareStatusType, ShareStatusTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "SHARE_STATUS_TYPE";

    public ShareStatusTypeService(
            ShareStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ShareStatusType.class,
                ShareStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
