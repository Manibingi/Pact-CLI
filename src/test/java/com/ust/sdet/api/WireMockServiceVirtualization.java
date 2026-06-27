package com.ust.sdet.api;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import static io.restassured.RestAssured.given;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class WireMockServiceVirtualisationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private HttpClient client;

    @BeforeEach
    void pointConsumerAtWireMock() {
        io.restassured.RestAssured.baseURI = wm.baseUrl();
        client = HttpClient.newHttpClient();
    }


    @Test
    @DisplayName("Stub Inventory - Success and Out Of Stock")
    void stubInventoryTwoOutcomes() {
        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-9"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                {
                                  "sku":"SKU-9",
                                  "qty":5
                                }
                                """))
        );

        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-0"))
                        .willReturn(
                                aResponse()
                                        .withStatus(409)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                {
                                  "error":"OUT_OF_STOCK"
                                }
                                """))
        );

        given()
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("qty", equalTo(5))
                .body("sku", equalTo("SKU-9"));

        given()
                .when()
                .get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .body("error", equalTo("OUT_OF_STOCK"));

        wm.verify(exactly(1), getRequestedFor(urlPathEqualTo("/inventory/SKU-9")));
    }


    @Test
    @DisplayName("Delayed response with explicit timeout")
    void shouldTimeoutThenSucceed() throws Exception {

        wm.stubFor(get(urlPathEqualTo("/orders/slow"))
                .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withFixedDelay(3000)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                    {
                                      "status":"CONFIRMED"
                                    }
                                    """)
                        )
        );

        String url = wm.baseUrl() + "/orders/slow";

        HttpRequest timeoutRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(1))
                        .GET()
                        .build();

                assertThrows(HttpTimeoutException.class,
                        () -> client.send(timeoutRequest, ofString()));

        HttpRequest successRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        var response = client.send(successRequest, ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("make it stateful - PENDING then CONFIRMED")
    void fulfilmentScenario() {
        wm.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("CONFIRMED") // Transition the state
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status":"PENDING"
                                }
                                """))
        );

        wm.stubFor(get(urlPathEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("CONFIRMED")
                // No willSetStateTo() here, so it remains in CONFIRMED forever
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status":"CONFIRMED"
                                }
                                """))
        );

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @DisplayName("Checking with faulty api malformed response")
    void testNetworkFaults() {
        wm.stubFor(get(urlPathEqualTo("/faulty-api"))
                .willReturn(aResponse()
                        .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        // Expect an IOException or similar when the connection abruptly fails
        HttpRequest faultyRequest = HttpRequest.newBuilder()
                .uri(URI.create(wm.baseUrl() + "/faulty-api"))
                .GET()
                .build();

        assertThrows(IOException.class, () -> {
            client.send(faultyRequest, ofString());
        });
    }
}