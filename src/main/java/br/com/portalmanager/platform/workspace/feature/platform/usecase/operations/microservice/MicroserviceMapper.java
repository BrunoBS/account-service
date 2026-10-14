package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.MicroserviceDetails;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.UpdateMicroserviceInput;
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

    public MicroserviceDetails toDetails(UpdateMicroserviceInput input) {
        return new MicroserviceDetails(input.name(), input.description(), input.settings());
    }

    public MicroserviceOutput toOutput(Microservice microservice) {
        return new MicroserviceOutput(
            microservice.getVersion(), microservice.getIdentifier(), microservice.getCode(),
            microservice.getName(), microservice.getDescription(), readSettings(microservice.getSettings()),
            microservice.getLifecycle().value(), microservice.getCreatedAt(), microservice.getUpdatedAt()
        );
    }

    private JsonNode readSettings(String settings) {
        try {
            JsonNode parsedSettings = objectMapper.readTree(settings);
            if (parsedSettings == null || !parsedSettings.isObject()) {
                throw new ApiException(PlatformMessageKeys.MICROSERVICE_SETTINGS_INVALID);
            }
            return parsedSettings;
        } catch (JacksonException exception) {
            throw new ApiException(PlatformMessageKeys.MICROSERVICE_SETTINGS_INVALID, exception);
        }
    }
}
