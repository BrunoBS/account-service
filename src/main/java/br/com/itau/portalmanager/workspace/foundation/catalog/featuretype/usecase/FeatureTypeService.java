package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.repository.FeatureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.usecase.ServiceTypeService;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class FeatureTypeService extends EnumCatalogService<FeatureType, FeatureTypeEnum> {

    private final FeatureTypeRepository repository;

    public FeatureTypeService(
            FeatureTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator,
            ServiceTypeService serviceTypeService) {
        super(
                repository,
                objectMapper,
                FeatureType.class,
                FeatureTypeEnum.class,
                (dto, result) -> {
                    JsonNode settings = dto.settings();
                    settingsValidator.validateSettings(settings, result);

                    if (settings == null || !settings.isObject()) {
                        result.addError("settings", "validation.settings.invalid");
                        return;
                    }

                    validateRequiredText(settings, "service", result);
                    validateQuarantine(settings, result);
                    validateAudit(settings, result);
                    validatePurge(settings, result);

                    JsonNode serviceNode = settings.get("service");
                    if (serviceNode != null && serviceNode.isTextual()
                            && !serviceNode.asText().isBlank()
                            && !serviceTypeService.existsActive(serviceNode.asText())) {
                        result.addError("settings.service", "validation.feature.service.invalid", serviceNode.asText());
                    }
                }
        );
        this.repository = repository;
    }

    private static void validateQuarantine(JsonNode settings, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode quarantine = requiredObject(settings, "quarantine", result);
        if (quarantine == null) return;
        validateBoolean(quarantine, "enabled", "settings.quarantine.enabled", result);
        validateBoolean(quarantine, "restoreAllowed", "settings.quarantine.restoreAllowed", result);
        JsonNode retention = quarantine.get("retentionDays");
        if (retention == null || !retention.isIntegralNumber() || retention.asInt() < 0) {
            result.addError("settings.quarantine.retentionDays", "validation.settings.retention-days.invalid");
        }
    }

    private static void validateAudit(JsonNode settings, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode audit = requiredObject(settings, "audit", result);
        if (audit == null) return;
        validateBoolean(audit, "enabled", "settings.audit.enabled", result);
        validateBoolean(audit, "snapshotOnPurge", "settings.audit.snapshotOnPurge", result);
    }

    private static void validatePurge(JsonNode settings, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode purge = requiredObject(settings, "purge", result);
        if (purge == null) return;
        validateBoolean(purge, "enabled", "settings.purge.enabled", result);
    }

    private static JsonNode requiredObject(JsonNode parent, String field, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isObject()) {
            result.addError("settings." + field, "validation.settings.object.required");
            return null;
        }
        return node;
    }

    private static void validateRequiredText(JsonNode parent, String field, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            result.addError("settings." + field, "validation.settings.text.required");
        }
    }

    private static void validateBoolean(JsonNode parent, String field, String path, br.com.portalmanager.platform.catalog.validation.CatalogValidationResult result) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isBoolean()) {
            result.addError(path, "validation.settings.boolean.required");
        }
    }

    public List<FeatureType> findActiveByNames(List<String> names) {
        return repository.findByCodeInAndActiveTrue(names);
    }
}
