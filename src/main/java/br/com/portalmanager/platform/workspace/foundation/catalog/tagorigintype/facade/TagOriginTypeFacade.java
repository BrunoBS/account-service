package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.usecase.TagOriginTypeService;
import org.springframework.stereotype.Component;

@Component
public class TagOriginTypeFacade extends AbstractCatalogFacade<TagOriginType> {

    public TagOriginTypeFacade(TagOriginTypeService service) {
        super(service);
    }
}
