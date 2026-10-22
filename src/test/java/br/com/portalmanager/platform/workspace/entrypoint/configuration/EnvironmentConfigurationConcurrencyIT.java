package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class EnvironmentConfigurationConcurrencyIT extends EnvironmentConfigurationHttpFixture {
    @Test
    void onlyOneConcurrentCreationCanReserveTheSameOwnerEnvironment() throws Exception {
        for (boolean app : List.of(false, true)) {
            List<Integer> codes =
                    race(
                            () -> post(base(app), create(globalEnvironment)).extract().statusCode(),
                            () ->
                                    post(base(app), create(globalEnvironment))
                                            .extract()
                                            .statusCode());
            assertThat(codes).containsExactlyInAnyOrder(201, 409);
            get(base(app)).statusCode(200).body("page.totalElements", equalTo(1));
        }
    }

    @Test
    void onlyOneConcurrentUpdateCanUseTheSameVersionWithoutMixedPublisherSets() throws Exception {
        for (boolean app : List.of(false, true)) {
            String id =
                    post(base(app), create(globalEnvironment))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            Map<String, Object> first = update(0);
            first.put("settings", Map.of("winner", "http"));
            first.put("publishers", List.of(publisher(http, 1)));
            Map<String, Object> second = update(0);
            second.put("settings", Map.of("winner", "kafka"));
            second.put("publishers", List.of(publisher(kafka, 1)));
            assertThat(
                            race(
                                    () -> put(base(app) + "/" + id, first).extract().statusCode(),
                                    () -> put(base(app) + "/" + id, second).extract().statusCode()))
                    .containsExactlyInAnyOrder(200, 409);
            io.restassured.response.ExtractableResponse<io.restassured.response.Response> stored =
                    get(base(app) + "/" + id)
                            .statusCode(200)
                            .body("version", equalTo(1))
                            .body("publishers", hasSize(1))
                            .extract();
            String winner = stored.path("settings.winner");
            assertThat(stored.<String>path("publishers[0].publisherCode"))
                    .isEqualTo(winner.equals("http") ? http : kafka);
        }
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> one =
                    executor.submit(
                            () -> {
                                start.await();
                                return first.call();
                            });
            Future<Integer> two =
                    executor.submit(
                            () -> {
                                start.await();
                                return second.call();
                            });
            start.countDown();
            return List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS));
        }
    }
}
