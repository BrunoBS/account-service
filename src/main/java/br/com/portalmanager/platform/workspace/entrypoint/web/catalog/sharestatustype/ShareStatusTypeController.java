package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.sharestatustype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.facade.ShareStatusTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share-status-type")
public class ShareStatusTypeController extends CatalogController<ShareStatusType> {

    public ShareStatusTypeController(ShareStatusTypeFacade facade) {
        super(facade);
    }
}
