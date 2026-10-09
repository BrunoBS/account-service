package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.usecase.ShareStatusTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import org.springframework.stereotype.Component;

@Component
public class ShareStatusTypeFacade extends AbstractCatalogFacade<ShareStatusType> {
    public ShareStatusTypeFacade(ShareStatusTypeService service) {
        super(service);
    }
}
