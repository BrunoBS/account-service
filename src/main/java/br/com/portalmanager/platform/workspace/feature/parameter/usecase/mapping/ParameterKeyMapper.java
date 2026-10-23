package br.com.portalmanager.platform.workspace.feature.parameter.usecase.mapping;

import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyOutput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeCode;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ParameterKeyMapper {

    public ParameterKey toDomain(
            CreateParameterKeyInput input, Long microserviceId, LocalDateTime now) {
        return new ParameterKey(
                microserviceId,
                input.key(),
                input.description(),
                ParameterKeyTypeCode.of(input.type()),
                input.value(),
                now);
    }

    public ParameterKeyTypeCode toTypeCode(String type) {
        return ParameterKeyTypeCode.of(type);
    }

    public ParameterKeyMetadata toMetadata(ParameterKey resource, Reference reference) {
        return new ParameterKeyMetadata(
                resource.getVersion(),
                resource.getIdentifier(),
                reference.identifier(),
                resource.getKey(),
                reference.code() + "." + resource.getKey(),
                resource.getDescription(),
                resource.getType().value(),
                resource.getStatus().value(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }

    public ParameterKeyOutput toOutput(ParameterKeyReadProjection resource, Reference reference) {
        return new ParameterKeyOutput(
                resource.getVersion(),
                resource.getIdentifier(),
                reference.identifier(),
                resource.getKey(),
                reference.code() + "." + resource.getKey(),
                resource.getDescription(),
                resource.getType(),
                resource.getValue().deepCopy(),
                resource.getStatus(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
