package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.port.WorkspaceTypePort;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

class WorkspaceSensitiveChangesTest {

    private final WorkspaceRepository repository = mock(WorkspaceRepository.class);
    private final WorkspaceTypePort workspaceTypeService = mock(WorkspaceTypePort.class);
    private final WorkspaceValidator validator =
            new WorkspaceValidator(workspaceTypeService, repository);
    private final Workspace workspace =
            new Workspace(
                    new WorkspaceDetails(
                            WorkspaceTypeCode.of("ADMIN"),
                            "Workspace Security",
                            "Security validation workspace",
                            "requester",
                            "SEC",
                            null,
                            "TEAM_A",
                            "workspace@example.com"),
                    LocalDateTime.now());

    @BeforeEach
    void initializeVersionAndCatalog() {
        ReflectionTestUtils.setField(workspace, "version", 0L);
        when(workspaceTypeService.existsActive("ADMIN")).thenReturn(true);
        when(workspaceTypeService.existsActive("MANAGER")).thenReturn(true);
    }

    @Test
    void shouldRejectStaleVersionBeforePermissionAndDatabaseLookups() {
        ReflectionTestUtils.setField(workspace, "version", 1L);
        assertThatThrownBy(() -> validator.validateForUpdate(workspace, input("MANAGER", "TEAM_B")))
                .isInstanceOf(ResourceVersionConflictException.class);
        verifyNoInteractions(repository, workspaceTypeService);
    }

    @AfterEach
    void clearSession() {
        UserContext.clear();
    }

    @Test
    void shouldDenyPrivilegedCreationWithoutSession() {
        UserContext.clear();
        assertThatThrownBy(() -> validator.validateForCreate(createInput("ADMIN", null)))
                .isInstanceOf(ForbiddenAccessException.class);
        assertThatThrownBy(() -> validator.validateForCreate(createInput("MANAGER", "TEAM_A")))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    private CreateWorkspaceInput createInput(String type, String group) {
        UpdateWorkspaceInput input = input(type, group);
        return new CreateWorkspaceInput(
                null,
                input.workspaceType(),
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.authorizerGroup(),
                input.settings(),
                input.emailGroup(),
                input.approvers(),
                input.tags());
    }

    @Test
    void shouldDenySensitiveChangesWithoutSession() {
        UserContext.clear();
        assertThatThrownBy(() -> validator.validateForUpdate(workspace, input("MANAGER", "TEAM_A")))
                .isInstanceOf(ForbiddenAccessException.class);
        assertThatThrownBy(() -> validator.validateForUpdate(workspace, input("ADMIN", null)))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void shouldDenySensitiveChangesToNonOwnerSession() {
        UserSession session = new UserSession();
        session.setGroups(Set.of("USER"));
        UserContext.set(session);
        assertThatThrownBy(() -> validator.validateForUpdate(workspace, input("MANAGER", "TEAM_A")))
                .isInstanceOf(ForbiddenAccessException.class);
        assertThatThrownBy(() -> validator.validateForUpdate(workspace, input("ADMIN", "TEAM_B")))
                .isInstanceOf(ForbiddenAccessException.class);
        assertThatCode(() -> validator.validateForUpdate(workspace, input("ADMIN", "TEAM_A")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOwnerSensitiveChanges() {
        UserSession session = new UserSession();
        session.setGroups(Set.of("PM5_OWNER"));
        UserContext.set(session);
        assertThatCode(() -> validator.validateForUpdate(workspace, input("MANAGER", "TEAM_B")))
                .doesNotThrowAnyException();
    }

    private UpdateWorkspaceInput input(String type, String group) {
        return new UpdateWorkspaceInput(
                0L,
                type,
                "Workspace Security",
                "Security validation workspace",
                "requester",
                "SEC",
                group,
                null,
                "workspace@example.com",
                List.of(new ApproverInput("F1234", "approver@example.com")),
                List.of());
    }
}
