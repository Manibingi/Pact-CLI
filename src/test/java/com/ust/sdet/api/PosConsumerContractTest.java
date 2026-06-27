package com.ust.sdet.api;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "oms-provider", pactVersion = PactSpecVersion.V4)
public class PosConsumerContractTest {

    // =========================================================================
    // PACT 1: GET INVENTORY (SUCCESS)
    // =========================================================================

    @Pact(consumer = "pos-consumer")
    public V4Pact createPactForInventorySuccess(PactBuilder builder) {
        return builder
                .given("Inventory exists for SKU-9")
                .expectsToReceiveHttpInteraction("A valid request for SKU-9 inventory", http -> http
                        .withRequest(req -> req
                                .method("GET")
                                .path("/inventory/SKU-9")
                        )
                        .willRespondWith(res -> res
                                .status(200)
                                .header("Content-Type", "application/json")
                                .body("{\"sku\":\"SKU-9\",\"qty\":5}")
                        )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactForInventorySuccess")
    @DisplayName("Verify Contract: GET Inventory Success")
    void testInventorySuccessContract(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("qty", equalTo(5));
    }

    // PACT 2: GET INVENTORY (OUT OF STOCK)

    @Pact(consumer = "pos-consumer")
    public V4Pact createPactForInventoryOutOfStock(PactBuilder builder) {
        return builder
                .given("Inventory is depleted for SKU-0")
                .expectsToReceiveHttpInteraction("A request for out of stock inventory", http -> http
                        .withRequest(req -> req
                                .method("GET")
                                .path("/inventory/SKU-0")
                        )
                        .willRespondWith(res -> res
                                .status(409)
                                .header("Content-Type", "application/json")
                                .body("{\"error\":\"OUT_OF_STOCK\"}")
                        )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactForInventoryOutOfStock")
    @DisplayName("Verify Contract: GET Inventory Out of Stock")
    void testInventoryOutOfStockContract(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .when()
                .get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .body("error", equalTo("OUT_OF_STOCK"));
    }


    // =========================================================================
    // PACT 3: POST CREATE ORDER
    // =========================================================================

    @Pact(consumer = "pos-consumer")
    public V4Pact createPactForOrderCreation(PactBuilder builder) {
        return builder
                .given("System is ready to receive new orders")
                .expectsToReceiveHttpInteraction("A request to create a new order", http -> http
                        .withRequest(req -> req
                                .method("POST")
                                .path("/orders")
                                .header("Content-Type", "application/json")
                                .body("{\"itemId\": \"SKU-9\", \"quantity\": 2}")
                        )
                        .willRespondWith(res -> res
                                .status(201)
                                .header("Content-Type", "application/json")
                                .body("{\"status\":\"PENDING\", \"orderId\":\"12345\"}")
                        )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactForOrderCreation")
    @DisplayName("Verify Contract: POST Create Order")
    void testCreateOrderContract(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .header("Content-Type", "application/json")
                .body("{\"itemId\": \"SKU-9\", \"quantity\": 2}")
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("orderId", equalTo("12345"));
    }
}