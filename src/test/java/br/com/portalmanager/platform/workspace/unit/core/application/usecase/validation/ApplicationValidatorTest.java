package br.com.portalmanager.platform.workspace.unit.core.application.usecase.validation;


import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.ApplicationScopeTypePort;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.ApplicationUsagePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ApplicationValidatorTest {

    private static final Long WORKSPACE_ID = 11L;
    private static final Long APPLICATION_ID = 23L;

    private final ApplicationScopeTypePort scopeTypes = mock(ApplicationScopeTypePort.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationUsagePort applicationUsage = mock(ApplicationUsagePort.class);
    private final ApplicationValidator validator =
            new ApplicationValidator(scopeTypes, applications, applicationUsage);

    @Test
    void rejectsNullCreateAndVersionOnCreate() {
        assertThrows(ValidationException.class, () -> validator.validateForCreate("MANAGER", WORKSPACE_ID, null));
        assertThrows(
                ValidationException.class,
                () -> validator.validateForCreate("MANAGER", WORKSPACE_ID, create(1L, "Application", null)));
    }

    @Test
    void validatesWorkspaceDuplicateAndOptionalScopeOnCreate() {
        when(applications.existsByWorkspaceIdAndName(WORKSPACE_ID, "Application")).thenReturn(false);
        assertDoesNotThrow(
                () -> validator.validateForCreate("MANAGER", WORKSPACE_ID, create(null, "Application", null)));
        verify(scopeTypes, never()).existsActive("BACKEND");

        when(applications.existsByWorkspaceIdAndName(WORKSPACE_ID, "Application")).thenReturn(true);
        when(scopeTypes.existsActive("BACKEND")).thenReturn(false);
        assertThrows(
                ValidationException.class,
                () -> validator.validateForCreate("ADMIN", WORKSPACE_ID, create(null, "Application", "BACKEND")));
    }

    @Test
    void rejectsNullUpdateAndMissingVersion() {
        Application application = application();
        assertThrows(ValidationException.class, () -> validator.validateForUpdate("MANAGER", application, null));
        assertThrows(
                ValidationException.class,
                () -> validator.validateForUpdate("MANAGER", application, update(null, "Application", "BACKEND")));
    }

    @Test
    void skipsDuplicateLookupWhenNameIsUnchangedAndAllowsSameScope() {
        Application application = application();
        when(scopeTypes.existsActive("BACKEND")).thenReturn(true);

        assertDoesNotThrow(
                () -> validator.validateForUpdate("MANAGER", application, update(1L, "Application", "BACKEND")));

        verify(applications, never())
                .existsByWorkspaceIdAndNameAndIdNot(WORKSPACE_ID, "Application", APPLICATION_ID);
        verify(applicationUsage, never()).isInUse(APPLICATION_ID);
    }

    @Test
    void rejectsDuplicateNameAndVersionConflictOnUpdate() {
        Application application = application();
        when(applications.existsByWorkspaceIdAndNameAndIdNot(WORKSPACE_ID, "Duplicate", APPLICATION_ID))
                .thenReturn(true);
        when(scopeTypes.existsActive("BACKEND")).thenReturn(true);

        assertThrows(
                ValidationException.class,
                () -> validator.validateForUpdate("MANAGER", application, update(1L, "Duplicate", "BACKEND")));
        assertThrows(
                ResourceVersionConflictException.class,
                () -> validator.validateForUpdate("MANAGER", application, update(2L, "Application", "BACKEND")));
    }

    @Test
    void blocksScopeChangeWhenApplicationHasSharingContracts() {
        Application application = application();
        when(applications.existsByWorkspaceIdAndNameAndIdNot(WORKSPACE_ID, "Application", APPLICATION_ID))
                .thenReturn(false);
        when(scopeTypes.existsActive("FRONTEND")).thenReturn(true);
        when(applicationUsage.isInUse(APPLICATION_ID)).thenReturn(true);

        assertThrows(
                ValidationException.class,
                () -> validator.validateForUpdate("MANAGER", application, update(1L, "Application", "FRONTEND")));

        when(applicationUsage.isInUse(APPLICATION_ID)).thenReturn(false);
        assertDoesNotThrow(
                () -> validator.validateForUpdate("MANAGER", application, update(1L, "Application", "FRONTEND")));

        when(scopeTypes.existsActive("SHARED")).thenReturn(true);
        assertDoesNotThrow(
                () -> validator.validateForUpdate("MANAGER", application, update(1L, "Application", "SHARED")));
        verify(applicationUsage, times(2)).isInUse(APPLICATION_ID);
    }

    @Test
    void rejectsUnpagedOversizedAndUnsupportedPagination() {
        assertThrows(ValidationException.class, () -> validator.validatePagination(Pageable.unpaged()));
        assertThrows(
                ValidationException.class,
                () -> validator.validatePagination(PageRequest.of(Integer.MAX_VALUE, 2)));
        assertThrows(
                ValidationException.class,
                () -> validator.validatePagination(PageRequest.of(0, 10, Sort.by("settings"))));

        assertDoesNotThrow(
                () -> validator.validatePagination(PageRequest.of(0, 10, Sort.by("createdAt"))));
    }

    private static CreateApplicationInput create(Long version, String name, String scope) {
        return new CreateApplicationInput(version, name, "alias", null, "APP", scope, null, null, null);
    }

    private static UpdateApplicationInput update(Long version, String name, String scope) {
        return new UpdateApplicationInput(version, name, "alias", null, "APP", scope, null, null, null);
    }

    private static Application application() {
        Application application = mock(Application.class);
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(application.getName()).thenReturn("Application");
        when(application.getVersion()).thenReturn(1L);
        when(application.getApplicationScope()).thenReturn(ApplicationScopeTypeCode.backend());
        return application;
    }
}
