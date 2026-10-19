package br.com.portalmanager.platform.workspace.core.publisher.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherDetails;
import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import org.springframework.stereotype.Component;

@Component
public class PublisherMapper {

    public PublisherOutput toOutput(Publisher publisher) {
        return new PublisherOutput(publisher.getVersion(), publisher.getIdentifier(), publisher.getCode(),
            publisher.getName(), publisher.getDescription(), publisher.getScope().value(), publisher.isDeprecated(),
            publisher.getSettings(), publisher.getLifecycle().value(), publisher.getCreatedAt(), publisher.getUpdatedAt());
    }

    public PublisherDetails toDetails(CreatePublisherInput input) {
        return new PublisherDetails(
            input.name(),
            input.description(),
            ResourceScopeTypeCode.of(input.scope()),
            Boolean.TRUE.equals(input.deprecated()),
            input.settings()
        );
    }

    public PublisherDetails toDetails(UpdatePublisherInput input, boolean currentDeprecated) {
        return new PublisherDetails(
            input.name(),
            input.description(),
            ResourceScopeTypeCode.of(input.scope()),
            input.deprecated() == null ? currentDeprecated : input.deprecated(),
            input.settings()
        );
    }
}
