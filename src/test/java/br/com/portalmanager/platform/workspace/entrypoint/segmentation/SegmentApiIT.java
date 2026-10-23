package br.com.portalmanager.platform.workspace.entrypoint.segmentation;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category.CategoryQueryService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class SegmentApiIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{
    private static final String SEGMENTS = "/api/v1/segmentation/segments";
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @Autowired private CategoryQueryService categoryQueries;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        reset(auditAppender, segmentRepository, categoryRepository);
    }

    @Test
    void createsNormalizedGlobalResourcesWithPublicIdentitiesAndAuditSnapshots() {
        String segment = create(SEGMENTS, payload("  MOBILE-PF  ", "  Mobile PF  "));
        String category =
                create(
                        categories(segment.toUpperCase()),
                        categoryPayload(" PREMIUM ", " Premium "));
        request()
                .get(SEGMENTS + "/" + segment)
                .then()
                .statusCode(200)
                .body("code", equalTo("mobile-pf"))
                .body("name", equalTo("Mobile PF"))
                .body("version", equalTo(0))
                .body("lifecycle", equalTo("ACTIVE"))
                .body("$", not(hasKey("id")));
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("code", equalTo("premium"))
                .body("name", equalTo("Premium"))
                .body("segmentIdentifier", equalTo(segment))
                .body("available", equalTo(true))
                .body("$", not(hasKey("segmentId")));
        assertThat(UUID.fromString(segment)).isNotNull();
        assertThat(UUID.fromString(category)).isNotNull();
        assertSnapshot("SEGMENT_CREATED", segment, "ACTIVE", 0);
        assertSnapshot("CATEGORY_CREATED", category, "ACTIVE", 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workspaces", Integer.class)).isZero();
    }

    @Test
    void centralIntegrityHandlerReturnsConflictWithoutExposingSqlAndRollsBackRejectedCreates() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        create(categories(segment), categoryPayload("premium", "Premium"));
        Long segmentId =
                jdbc.queryForObject(
                        "SELECT id FROM segment WHERE identifier=?", Long.class, segment);
        // Simulate prechecks passing before a competing insert commits.
        doReturn(false).when(segmentRepository).existsByCode("mobile-pf");
        doReturn(false).when(categoryRepository).existsBySegmentIdAndCode(segmentId, "premium");

        io.restassured.response.Response segmentConflict =
                request()
                        .contentType(ContentType.JSON)
                        .body(payload("mobile-pf", "Duplicate"))
                        .post(SEGMENTS);
        io.restassured.response.Response categoryConflict =
                request()
                        .contentType(ContentType.JSON)
                        .body(categoryPayload("premium", "Duplicate"))
                        .post(categories(segment));
        for (io.restassured.response.Response response :
                List.of(segmentConflict, categoryConflict)) {
            response.then().statusCode(409).body("code", equalTo("GLOBAL-0004"));
            assertThat(response.asString()).doesNotContain("SQL", "Duplicate entry", "uk_segment");
            assertThat(response.jsonPath().getString("message")).doesNotContain("1062");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM segment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void reservesSegmentCodesGloballyAndCategoryCodesPerSegmentIncludingInactiveRecords() {
        String first = create(SEGMENTS, payload("mobile-pf", "Same name"));
        String second = create(SEGMENTS, payload("internet-pf", "Same name"));
        String category = create(categories(first), categoryPayload("premium", "Same name"));
        create(categories(second), categoryPayload("premium", "Same name"));
        request().patch(categories(first) + "/" + category + "/inactivate").then().statusCode(200);
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("premium", "Other"))
                .post(categories(first))
                .then()
                .statusCode(409)
                .body("code", equalTo("SEGMENT-0004"));
        request().patch(SEGMENTS + "/" + first + "/inactivate").then().statusCode(200);
        request()
                .contentType(ContentType.JSON)
                .body(payload("mobile-pf", "Other"))
                .post(SEGMENTS)
                .then()
                .statusCode(409)
                .body("code", equalTo("SEGMENT-0003"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void managesCategoriesIndependentlyOfTheirParentAndReportsEffectiveAvailability() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        request().patch(SEGMENTS + "/" + segment + "/inactivate").then().statusCode(200);
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("ACTIVE"))
                .body("available", equalTo(false))
                .body("version", equalTo(0));
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("basic", "Basic"))
                .post(categories(segment))
                .then()
                .statusCode(400)
                .body("code", equalTo("SEGMENT-0103"));
        request()
                .patch(categories(segment) + "/" + category + "/inactivate")
                .then()
                .statusCode(200);
        request()
                .patch(categories(segment) + "/" + category + "/activate")
                .then()
                .statusCode(200)
                .body("available", equalTo(false));
        request().patch(SEGMENTS + "/" + segment + "/activate").then().statusCode(200);
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("available", equalTo(true));
        request()
                .patch(categories(segment) + "/" + category + "/inactivate")
                .then()
                .statusCode(200);
        request().patch(SEGMENTS + "/" + segment + "/inactivate").then().statusCode(200);
        request().patch(SEGMENTS + "/" + segment + "/activate").then().statusCode(200);
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"))
                .body("available", equalTo(false));
        assertSnapshot("CATEGORY_ACTIVATED", category, "ACTIVE", 2);
    }

    @Test
    void administrativeDetailAndUpdateRemainAvailableForInactiveResources() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            request().patch(route + "/inactivate").then().statusCode(200);
            Map<String, Object> update = update(route, "Renamed while inactive");
            request()
                    .contentType(ContentType.JSON)
                    .body(update)
                    .put(route)
                    .then()
                    .statusCode(200)
                    .body("lifecycle", equalTo("INACTIVE"))
                    .body("name", equalTo("Renamed while inactive"))
                    .body("version", equalTo(2));
        }
    }

    @Test
    void paginatesFiltersAndSortsUsingDatabaseQueriesForBothResources() {
        String first = create(SEGMENTS, payload("mobile-pf", "A Mobile"));
        String second = create(SEGMENTS, payload("internet-pf", "Z Internet"));
        String premium = create(categories(first), categoryPayload("premium", "Z Premium"));
        String basic = create(categories(first), categoryPayload("basic", "A Basic"));
        create(categories(second), categoryPayload("premium", "Premium"));
        request()
                .get(SEGMENTS + "?size=1&sort=name,desc")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(2))
                .body("content[0].identifier", equalTo(second));
        request()
                .get(SEGMENTS + "?size=1&page=1&sort=name,desc")
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(first));
        request()
                .queryParam("code", " MOBILE-PF ")
                .get(SEGMENTS)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1));
        request()
                .queryParam("name", " mobile ")
                .get(SEGMENTS)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1));
        request()
                .queryParam("name", "%")
                .get(SEGMENTS)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
        request()
                .get(categories(first) + "?sort=name,desc&size=1")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(2))
                .body("content[0].identifier", equalTo(premium));
        request().patch(SEGMENTS + "/" + first + "/inactivate").then().statusCode(200);
        request().patch(categories(first) + "/" + basic + "/inactivate").then().statusCode(200);
        request().get(SEGMENTS).then().statusCode(200).body("totalElements", equalTo(1));
        request()
                .queryParam("lifecycle", " inactive ")
                .get(SEGMENTS)
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(first));
        request()
                .get(categories(first) + "?lifecycle=INACTIVE")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].identifier", equalTo(basic));
        request()
                .get(categories(first))
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(premium))
                .body("content[0].available", equalTo(false));
        request().get(categories(UUID.randomUUID().toString())).then().statusCode(404);
        request().get(categories("invalid")).then().statusCode(400);
        for (String route : List.of(SEGMENTS, categories(first))) {
            for (String query :
                    List.of(
                            "sort=segment.id",
                            "lifecycle=unknown",
                            "lifecycle=",
                            "lifecycle=QUARANTINED",
                            "page=2147483647&size=100")) {
                request().get(route + "?" + query).then().statusCode(400);
            }
        }
    }

    @Test
    void categoryPaginationDoesNotLoadTheParentWithAdditionalQueriesPerItem() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        for (int index = 0; index < 3; index++) {
            create(categories(segment), categoryPayload("category-" + index, "Category " + index));
        }
        org.hibernate.stat.Statistics statistics =
                entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean originallyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(
                            categoryQueries
                                    .findAll(
                                            segment,
                                            null,
                                            org.springframework.data.domain.PageRequest.of(0, 2))
                                    .getContent())
                    .hasSize(2);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
            assertThat(statistics.getEntityFetchCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(originallyEnabled);
        }
    }

    @Test
    void validatesOriginalJsonBeforeCoercionOnCreateAndUpdate() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        for (String route : List.of(SEGMENTS, categories(segment))) {
            String identifier = route.equals(SEGMENTS) ? segment : category;
            Map<String, Object> valid =
                    route.equals(SEGMENTS)
                            ? payload("new-code", "Valid")
                            : categoryPayload("new-code", "Valid");
            for (Map<String, Object> change :
                    List.<Map<String, Object>>of(
                            Map.of("code", 123),
                            Map.of("name", true),
                            Map.of("code", "a.b"),
                            Map.of("code", "a_b"),
                            Map.of("code", "a b"),
                            Map.of("code", "-a"),
                            Map.of("code", "a--b"),
                            Map.of("name", "  "),
                            Map.of("code", "a".repeat(101)),
                            Map.of("name", "n".repeat(151)),
                            Map.of("identifier", UUID.randomUUID().toString()),
                            Map.of("id", 1),
                            Map.of("lifecycle", "ACTIVE"),
                            Map.of("version", "0"),
                            Map.of("version", 0.5),
                            Map.of("version", -1))) {
                Map<String, Object> body = new HashMap<>(valid);
                body.putAll(change);
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post(route)
                        .then()
                        .statusCode(400);
                body.putIfAbsent("version", 0);
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .put(route + "/" + identifier)
                        .then()
                        .statusCode(400);
            }
            for (String invalid :
                    List.of(
                            "null",
                            "[]",
                            "{\"code\":\"x\"}",
                            "{\"name\":\"X\"}",
                            "{\"code\":null,\"name\":\"X\"}")) {
                request()
                        .contentType(ContentType.JSON)
                        .body(invalid)
                        .post(route)
                        .then()
                        .statusCode(400);
                request()
                        .contentType(ContentType.JSON)
                        .body(invalid)
                        .put(route + "/" + identifier)
                        .then()
                        .statusCode(400);
            }
        }
        for (Object parent : List.of(123, "not-a-uuid", Map.of("identifier", segment))) {
            Map<String, Object> body = categoryPayload("basic", "Basic");
            body.put("segmentIdentifier", parent);
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(categories(segment))
                    .then()
                    .statusCode(400);
            body.put("version", 0);
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put(categories(segment) + "/" + category)
                    .then()
                    .statusCode(400);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM segment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void acceptsSpecifiedLimitsAndNumericInitialCodeButNeverStoresACompositeCategoryCode() {
        String segment = create(SEGMENTS, payload("1".repeat(100), "n".repeat(150)));
        String category =
                create(categories(segment), categoryPayload("2".repeat(100), "m".repeat(150)));
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("code", equalTo("2".repeat(100)));
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("mobile-pf.premium", "Premium"))
                .post(categories(segment))
                .then()
                .statusCode(400);
    }

    @Test
    void protectsVersionsCodesAndThePermanentCategoryParent() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String other = create(SEGMENTS, payload("internet-pf", "Internet"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            Map<String, Object> body = update(route, "Renamed");
            body.remove("version");
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put(route)
                    .then()
                    .statusCode(400)
                    .body("details.field", hasItem("version"));
            body.put("version", 0);
            String code = (String) body.put("code", "changed");
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put(route)
                    .then()
                    .statusCode(400)
                    .body("details.field", hasItem("code"));
            body.put("code", code);
            if (route.startsWith(categories(segment))) {
                body.put("segmentIdentifier", other);
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .put(route)
                        .then()
                        .statusCode(400)
                        .body("details.field", hasItem("segmentIdentifier"));
                body.remove("segmentIdentifier");
            }
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put(route)
                    .then()
                    .statusCode(200)
                    .body("version", equalTo(1));
            request()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put(route)
                    .then()
                    .statusCode(409)
                    .body("code", equalTo("GLOBAL-0009"));
            request()
                    .get(route)
                    .then()
                    .statusCode(200)
                    .body("identifier", equalTo(route.substring(route.lastIndexOf('/') + 1)));
        }
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "code", "new", "name", "New"))
                .post(SEGMENTS)
                .then()
                .statusCode(400);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "code", "new", "name", "New"))
                .post(categories(segment))
                .then()
                .statusCode(400);
        assertSnapshot("SEGMENT_UPDATED", segment, "ACTIVE", 1);
        assertSnapshot("CATEGORY_UPDATED", category, "ACTIVE", 1);
    }

    @Test
    void requiresOwnerForEveryMutationAndAllowsAuthenticatedGlobalCatalogReads() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_DEV"));
        authorization.forbiddenResource(resource -> resource.header("policy", "OWNER"));
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            request().get(route).then().statusCode(200);
            Map<String, Object> body = update(route, "Denied");
            request().contentType(ContentType.JSON).body(body).put(route).then().statusCode(403);
            request().patch(route + "/activate").then().statusCode(403);
            request().patch(route + "/inactivate").then().statusCode(403);
        }
        request()
                .contentType(ContentType.JSON)
                .body(payload("denied", "Denied"))
                .post(SEGMENTS)
                .then()
                .statusCode(403);
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("denied", "Denied"))
                .post(categories(segment))
                .then()
                .statusCode(403);
        for (String route : List.of(SEGMENTS, categories(segment))) {
            request().get(route).then().statusCode(200);
            authorization.verifyCalledWithPolicy("OPEN");
            given().port(port).get(route).then().statusCode(401);
        }
        authorization.reset();
        authorization.forbidden();
        request().get(SEGMENTS).then().statusCode(403);
        request().get(categories(segment) + "/" + category).then().statusCode(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void exposesNoDeleteAndRejectsMissingResourcesOrInvalidTransitions() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        for (String route : List.of(SEGMENTS, categories(segment))) {
            String identifier = route.equals(SEGMENTS) ? segment : category;
            request().delete(route + "/" + identifier).then().statusCode(405);
            request().get(route + "/" + UUID.randomUUID()).then().statusCode(404);
            request().patch(route + "/" + UUID.randomUUID() + "/activate").then().statusCode(404);
            request().patch(route + "/" + identifier + "/activate").then().statusCode(400);
            request().patch(route + "/" + identifier + "/inactivate").then().statusCode(200);
            request().patch(route + "/" + identifier + "/inactivate").then().statusCode(400);
        }
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("basic", "Basic"))
                .post(categories(UUID.randomUUID().toString()))
                .then()
                .statusCode(404)
                .body("code", equalTo("SEGMENT-0001"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void translatesBusinessMessagesUsingTheRequestedLocale() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        request()
                .header("Accept-Language", "en-US")
                .contentType(ContentType.JSON)
                .body(payload("mobile-pf", "Duplicate"))
                .post(SEGMENTS)
                .then()
                .statusCode(409)
                .body("message", equalTo("A segment with this code already exists."));
        request()
                .header("Accept-Language", "pt-BR")
                .contentType(ContentType.JSON)
                .body(payload("mobile-pf", "Duplicado"))
                .post(SEGMENTS)
                .then()
                .statusCode(409)
                .body("message", equalTo("Já existe um segmento com este código."));
        request()
                .header("Accept-Language", "en-US")
                .get(categories(segment) + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("message", equalTo("Segment category not found."));
        for (String route : List.of(SEGMENTS, categories(segment))) {
            for (String lifecycle : List.of("unknown", "", "QUARANTINED")) {
                request()
                        .header("Accept-Language", "en-US")
                        .queryParam("lifecycle", lifecycle)
                        .get(route)
                        .then()
                        .statusCode(400)
                        .body("code", equalTo("SEGMENT-0111"))
                        .body("message", equalTo("Use ACTIVE or INACTIVE for this catalog."));
                request()
                        .header("Accept-Language", "pt-BR")
                        .queryParam("lifecycle", lifecycle)
                        .get(route)
                        .then()
                        .statusCode(400)
                        .body("code", equalTo("SEGMENT-0111"))
                        .body("message", equalTo("Use ACTIVE ou INACTIVE neste catálogo."));
            }
        }
    }

    @Test
    void auditFailuresRollBackCreationUpdatesAndLifecycleForBothResources() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        doThrow(new IllegalStateException("Test audit failure"))
                .when(auditAppender)
                .append(any(), any());
        request()
                .contentType(ContentType.JSON)
                .body(payload("rollback", "Rollback"))
                .post(SEGMENTS)
                .then()
                .statusCode(500);
        request()
                .contentType(ContentType.JSON)
                .body(categoryPayload("rollback", "Rollback"))
                .post(categories(segment))
                .then()
                .statusCode(500);
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            Map<String, Object> before =
                    request()
                            .get(route)
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeRef<Map<String, Object>>() {});
            request()
                    .contentType(ContentType.JSON)
                    .body(update(route, "Rollback"))
                    .put(route)
                    .then()
                    .statusCode(500);
            request().patch(route + "/inactivate").then().statusCode(500);
            Map<String, Object> after =
                    request()
                            .get(route)
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeRef<Map<String, Object>>() {});
            assertThat(after).isEqualTo(before);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM segment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(2);
        reset(auditAppender);
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            request().patch(route + "/inactivate").then().statusCode(200);
        }
        doThrow(new IllegalStateException("Test audit failure"))
                .when(auditAppender)
                .append(any(), any());
        for (String route :
                List.of(SEGMENTS + "/" + segment, categories(segment) + "/" + category)) {
            Map<String, Object> before =
                    request()
                            .get(route)
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeRef<Map<String, Object>>() {});
            request().patch(route + "/activate").then().statusCode(500);
            Map<String, Object> after =
                    request()
                            .get(route)
                            .then()
                            .statusCode(200)
                            .extract()
                            .as(new TypeRef<Map<String, Object>>() {});
            assertThat(after).isEqualTo(before);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(4);
    }

    @Test
    void categoryPathsCannotReadOrMutateAResourceBelongingToAnotherSegment() {
        String segment = create(SEGMENTS, payload("mobile-pf", "Mobile"));
        String other = create(SEGMENTS, payload("internet-pf", "Internet"));
        String category = create(categories(segment), categoryPayload("premium", "Premium"));
        String wrongPath = categories(other) + "/" + category;
        request().get(wrongPath).then().statusCode(404);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "code", "premium", "name", "Denied"))
                .put(wrongPath)
                .then()
                .statusCode(404);
        request().patch(wrongPath + "/activate").then().statusCode(404);
        request().patch(wrongPath + "/inactivate").then().statusCode(404);
        request().get(categories(other)).then().statusCode(200).body("totalElements", equalTo(0));
        request()
                .queryParam("segmentIdentifier", segment)
                .get(categories(other))
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
        request()
                .get(categories(segment) + "/" + category)
                .then()
                .statusCode(200)
                .body("name", equalTo("Premium"))
                .body("version", equalTo(0))
                .body("lifecycle", equalTo("ACTIVE"));
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("code", "basic", "name", "Basic", "segmentIdentifier", other))
                .post(categories(segment))
                .then()
                .statusCode(400)
                .body("details.field", hasItem("segmentIdentifier"));
        request().get(categories("invalid")).then().statusCode(400);
    }

    private String categories(String segmentIdentifier) {
        return SEGMENTS + "/" + segmentIdentifier + "/categories";
    }

    private String create(String route, Map<String, Object> body) {
        return request()
                .contentType(ContentType.JSON)
                .body(body)
                .post(route)
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private Map<String, Object> payload(String code, String name) {
        return new HashMap<>(Map.of("code", code, "name", name));
    }

    private Map<String, Object> categoryPayload(String code, String name) {
        Map<String, Object> body = payload(code, name);
        return body;
    }

    private Map<String, Object> update(String route, String name) {
        Map<String, Object> current =
                request()
                        .get(route)
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(new TypeRef<Map<String, Object>>() {});
        Map<String, Object> body = payload((String) current.get("code"), name);
        body.put("version", current.get("version"));
        return body;
    }

    private RequestSpecification request() {
        return given().port(port)
                .header("Authorization", "Bearer segment-review")
                .header("correlation-id", "segment-review")
                .accept(ContentType.JSON);
    }

    private void assertSnapshot(String event, String identifier, String lifecycle, int version) {
        List<Map<String, Object>> events =
                jdbc.queryForList(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.identifier')) identifier,"
                            + " JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) lifecycle,"
                            + " JSON_EXTRACT(payload,'$.version') version FROM audit_outbox WHERE"
                            + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=?",
                        event);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst())
                .containsEntry("identifier", identifier)
                .containsEntry("lifecycle", lifecycle);
        assertThat(String.valueOf(events.getFirst().get("version")))
                .isEqualTo(String.valueOf(version));
    }
}
