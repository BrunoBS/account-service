package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.microservice;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.MicroserviceDetails;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MicroserviceMapper {

    private final ObjectMapper objectMapper;

    public MicroserviceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MicroserviceDetails toDetails(CreateMicroserviceInput input) {
        return new MicroserviceDetails(input.name(), input.description(), input.settings());
    }

    public Microservice toDomain(CreateMicroserviceInput input, String code, java.time.LocalDateTime now) {
        return new Microservice(code, toDetails(input), now);
    }

    public MicroserviceDetails toDetails(UpdateMicroserviceInput input) {
        return new MicroserviceDetails(input.name(), input.description(), input.settings());
    }

    public MicroserviceOutput toOutput(Microservice microservice) {
        return new MicroserviceOutput(
                microservice.getVersion(),
                microservice.getIdentifier(),
                microservice.getCode(),
                microservice.getName(),
                microservice.getDescription(),
                readSettings(microservice.getSettings()),
                microservice.getLifecycle().value(),
                microservice.getCreatedAt(),
                microservice.getUpdatedAt());
    }

    private JsonNode readSettings(String settings) {
        try {
            JsonNode parsedSettings = objectMapper.readTree(settings);
            if (parsedSettings == null || !parsedSettings.isObject()) {
                throw new ApiException(ServiceContextMessageKeys.MICROSERVICE_SETTINGS_INVALID);
            }
            return parsedSettings;
        } catch (JacksonException exception) {
            throw new ApiException(
                    ServiceContextMessageKeys.MICROSERVICE_SETTINGS_INVALID, exception);
        }
    }
}
