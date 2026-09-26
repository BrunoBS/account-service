package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.sharestatustype;

import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.usecase.ShareStatusTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share-status-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class ShareStatusTypeController extends CatalogController<ShareStatusType> {

    public ShareStatusTypeController(ShareStatusTypeService service) {
        super(service);
    }
}
