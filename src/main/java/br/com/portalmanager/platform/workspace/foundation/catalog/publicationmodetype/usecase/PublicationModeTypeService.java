package br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.repository.PublicationModeTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class PublicationModeTypeService extends EnumCatalogService<PublicationModeType, PublicationModeTypeEnum> {
    public PublicationModeTypeService(PublicationModeTypeRepository repository, ObjectMapper objectMapper,
                                      SchemaValidator schemaValidator) {
        super(repository, objectMapper, PublicationModeType.class, PublicationModeTypeEnum.class,
                "publication-mode-type", schemaValidator);
    }
}
