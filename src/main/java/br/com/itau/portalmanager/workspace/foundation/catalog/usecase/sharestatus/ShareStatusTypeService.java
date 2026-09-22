package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.sharestatus;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus.ShareStatusType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus.ShareStatusTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.sharestatus.ShareStatusTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShareStatusTypeService extends EnumCatalogService<ShareStatusType, ShareStatusTypeEnum> {

    public ShareStatusTypeService(
            ShareStatusTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ShareStatusType.class,
                ShareStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
