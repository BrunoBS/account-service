package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersion;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersionStatus;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchemaVersionCommandService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaValidator validator;

    public SchemaVersionCommandService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            SchemaValidator validator
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.validator = validator;
    }

    @Transactional
    public SchemaVersionOutput createDraft(
            String schemaIdentifier,
            CreateSchemaVersionInput input
    ) {
        Schema schema = schemaRepository.findByIdentifierForUpdate(schemaIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));

        JsonNode definition = input == null ? null : input.definition();
        ValidationResult validation = new ValidationResult();
        validator.validateSchemaSyntax(definition, validation);
        if (validation.hasErrors()) {
            throw new ValidationException(validation);
        }

        String serialized = validator.toJsonString(definition);
        List<SchemaVersion> versions = versionRepository.findAllForUpdate(schema.getId());

        if (!versions.isEmpty()) {
            SchemaVersion latest = versions.getFirst();
            if (validator.fromString(latest.getDefinition()).equals(definition)) {
                return SchemaVersionOutput.from(latest);
            }
        }

        int nextVersion = versions.isEmpty() ? 1 : versions.getFirst().getSchemaVersion() + 1;
        SchemaVersion created = versionRepository.save(new SchemaVersion(
                schema,
                nextVersion,
                input.versionName(),
                serialized,
                SchemaVersionStatus.DRAFT,
                LocalDateTime.now()
        ));
        return SchemaVersionOutput.from(created);
    }

    @Transactional
    public SchemaVersionOutput publish(
            String schemaIdentifier,
            String versionIdentifier
    ) {
        Schema schema = schemaRepository.findByIdentifierForUpdate(schemaIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));

        versionRepository.findAllForUpdate(schema.getId());

        SchemaVersion version = versionRepository
                .findByIdentifierAndSchema_Id(versionIdentifier, schema.getId())
                .orElseThrow(() -> new IllegalArgumentException("Schema version not found"));

        if (!version.isPublished()) {
            version.publish();
        }
        return SchemaVersionOutput.from(version);
    }
}
