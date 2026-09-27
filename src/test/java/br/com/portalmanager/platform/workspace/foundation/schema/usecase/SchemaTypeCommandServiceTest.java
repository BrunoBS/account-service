package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeCommandService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SchemaTypeCommandServiceTest {

    private final SchemaTypeRepository repository = mock(SchemaTypeRepository.class);
    private final SchemaRepository schemaRepository = mock(SchemaRepository.class);
    private final SchemaTypeCommandService service = new SchemaTypeCommandService(repository, schemaRepository);

    @Test
    void rejectsEmptyScopesOnCreate() {
        assertThatThrownBy(() -> service.create(new CreateSchemaTypeInput(
                "APPLICATION", "Application", null, Set.of()
        ))).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsInvalidScopeOnCreate() {
        assertThatThrownBy(() -> service.create(new CreateSchemaTypeInput(
                "APPLICATION", "Application", null, Set.of("TENANT")
        ))).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsDuplicateCode() {
        when(repository.existsByCode("APPLICATION")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSchemaTypeInput(
                "APPLICATION", "Application", null, Set.of("PLATFORM")
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsRemovingScopeThatIsAlreadyInUse() throws Exception {
        SchemaType schemaType = schemaType(
                "type-id",
                "APPLICATION",
                Set.of(SchemaScopeTypeCode.platform(), SchemaScopeTypeCode.workspace())
        );
        when(repository.findByIdentifier("type-id")).thenReturn(Optional.of(schemaType));
        when(schemaRepository.existsBySchemaTypeCodeAndScope("APPLICATION", "WORKSPACE"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(
                "type-id",
                new UpdateSchemaTypeInput(
                        schemaType.getVersion(),
                        "Application",
                        null,
                        Set.of("PLATFORM")
                )
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void allowsRemovingUnusedScope() throws Exception {
        SchemaType schemaType = schemaType(
                "type-id",
                "APPLICATION",
                Set.of(SchemaScopeTypeCode.platform(), SchemaScopeTypeCode.workspace())
        );
        when(repository.findByIdentifier("type-id")).thenReturn(Optional.of(schemaType));
        when(schemaRepository.existsBySchemaTypeCodeAndScope("APPLICATION", "WORKSPACE"))
                .thenReturn(false);

        service.update(
                "type-id",
                new UpdateSchemaTypeInput(
                        schemaType.getVersion(),
                        "Application",
                        null,
                        Set.of("PLATFORM")
                )
        );

        verify(schemaRepository)
                .existsBySchemaTypeCodeAndScope("APPLICATION", "WORKSPACE");
    }

    @Test
    void rejectsDeletingActiveType() throws Exception {
        SchemaType schemaType = schemaType(
                "type-id",
                "APPLICATION",
                Set.of(SchemaScopeTypeCode.platform())
        );
        when(repository.findByIdentifier("type-id")).thenReturn(Optional.of(schemaType));

        assertThatThrownBy(() -> service.delete("type-id"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsDeletingInactiveTypeStillInUse() throws Exception {
        SchemaType schemaType = schemaType(
                "type-id",
                "APPLICATION",
                Set.of(SchemaScopeTypeCode.platform())
        );
        schemaType.inactivate(LocalDateTime.now());
        when(repository.findByIdentifier("type-id")).thenReturn(Optional.of(schemaType));
        when(schemaRepository.existsBySchemaTypeCode("APPLICATION")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("type-id"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deletesInactiveUnusedType() throws Exception {
        SchemaType schemaType = schemaType(
                "type-id",
                "APPLICATION",
                Set.of(SchemaScopeTypeCode.platform())
        );
        schemaType.inactivate(LocalDateTime.now());
        when(repository.findByIdentifier("type-id")).thenReturn(Optional.of(schemaType));
        when(schemaRepository.existsBySchemaTypeCode("APPLICATION")).thenReturn(false);

        service.delete("type-id");

        verify(repository).delete(schemaType);
    }

    private SchemaType schemaType(
            String identifier,
            String code,
            Set<SchemaScopeTypeCode> scopes
    ) throws Exception {
        SchemaType schemaType = new SchemaType(
                code,
                "Application",
                null,
                scopes,
                LocalDateTime.now()
        );
        Field identifierField = SchemaType.class.getDeclaredField("identifier");
        identifierField.setAccessible(true);
        identifierField.set(schemaType, identifier);

        Field versionField = SchemaType.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(schemaType, 0L);
        return schemaType;
    }
}
