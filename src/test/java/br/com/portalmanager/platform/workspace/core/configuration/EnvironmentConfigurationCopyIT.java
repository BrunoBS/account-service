package br.com.portalmanager.platform.workspace.core.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;

import org.junit.jupiter.api.Test;

import java.util.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class EnvironmentConfigurationCopyIT extends EnvironmentConfigurationHttpFixture {
    @Test
    void copiesOnCreateAndUpdateWithoutCopyingSettingsOrSharingAssociations() {
        String origin =
                post(base(false), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        jdbc.update("update publishers set deprecated=true where identifier=?", http);
        Map<String, Object> copy = create(globalEnvironment);
        copy.remove("publishers");
        copy.put("copyWorkspacePublishers", true);
        copy.put("settings", Map.of("applicationOnly", true));
        String destination =
                post(base(true), copy)
                        .statusCode(201)
                        .body("settings.applicationOnly", equalTo(true))
                        .body("publishers.publisherIdentifier", contains(kafka, http))
                        .extract()
                        .path("identifier");
        Map<String, Object> originUpdate = update(0);
        originUpdate.put(
                "publishers",
                List.of(
                        Map.of(
                                "publisherIdentifier",
                                http,
                                "sortOrder",
                                5,
                                "parameters",
                                Map.of("channel", "updated", "onlySource", true))));
        originUpdate.put("settings", Map.of("workspaceOnly", true));
        put(base(false) + "/" + origin, originUpdate).statusCode(200);
        get(base(true) + "/" + destination)
                .statusCode(200)
                .body("publishers.publisherIdentifier", contains(kafka, http))
                .body("settings.applicationOnly", equalTo(true))
                .body("publishers[1].parameters.channel", equalTo("orders"))
                .body("publishers[1].parameters.onlySource", nullValue());
        Map<String, Object> recopy = update(0);
        recopy.remove("publishers");
        recopy.put("copyWorkspacePublishers", true);
        recopy.put("settings", Map.of("own", 123));
        put(base(true) + "/" + destination, recopy)
                .statusCode(200)
                .body("version", equalTo(1))
                .body("publishers.publisherIdentifier", contains(http))
                .body("publishers.sortOrder", contains(5))
                .body("publishers[0].parameters.channel", equalTo("updated"))
                .body("settings.own", equalTo(123))
                .body("settings.workspaceOnly", nullValue());
        put(base(true) + "/" + destination, recopy).statusCode(409);
        get(base(false) + "/" + origin).statusCode(200).body("version", equalTo(1));
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from"
                                        + " application_environment_configuration_publishers",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void validatesCopyMutualExclusionDefaultsAndMissingOrigin() {
        Map<String, Object> copy = create(globalEnvironment);
        copy.remove("publishers");
        copy.put("copyWorkspacePublishers", true);
        post(base(true), copy).statusCode(404);
        copy.put("publishers", List.of(publisher(http, 1)));
        post(base(true), copy).statusCode(400);
        copy.put("publishers", null);
        post(base(true), copy).statusCode(400);
        copy.remove("publishers");
        copy.put("copyWorkspacePublishers", false);
        post(base(true), copy).statusCode(400);
        copy.put("copyWorkspacePublishers", "true");
        post(base(true), copy).statusCode(400);
        post(base(false), create(globalEnvironment)).statusCode(201);
        copy.remove("copyWorkspacePublishers");
        post(base(true), copy).statusCode(400);
        copy.put("copyWorkspacePublishers", true);
        copy.put("environmentIdentifier", ownEnvironment);
        post(base(true), copy).statusCode(404);
        copy.put("environmentIdentifier", globalEnvironment);
        post(base(true), copy).statusCode(201);
    }

    @Test
    void rejectsInactiveSourcePublishersAndRollsBackReplacementCompletely() {
        post(base(false), create(globalEnvironment)).statusCode(201);
        String destination =
                post(base(true), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        jdbc.update("update publishers set lifecycle_code='INACTIVE' where identifier=?", http);
        Map<String, Object> copy = update(0);
        copy.remove("publishers");
        copy.put("copyWorkspacePublishers", true);
        put(base(true) + "/" + destination, copy).statusCode(400);
        get(base(true) + "/" + destination)
                .statusCode(200)
                .body("version", equalTo(0))
                .body("settings", anEmptyMap())
                .body("publishers.publisherIdentifier", contains(kafka, http));
        Map<String, Object> createCopy = create(globalEnvironment);
        createCopy.remove("publishers");
        createCopy.put("copyWorkspacePublishers", true);
        String secondApplication = UUID.randomUUID().toString();
        jdbc.update(
                "insert into"
                    + " applications(version,identifier,workspace_id,name,alias,acronym,application_scope_code,settings,lifecycle_code,created_at,updated_at)"
                    + " values (0,?,?,'Second"
                    + " app','second','APP','BACKEND','{}','ACTIVE',now(6),now(6))",
                secondApplication,
                workspaceId());
        post(base(true).replace(application, secondApplication), createCopy).statusCode(400);
    }
}
