package com.ust.sdet.api;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;

import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(PactConsumerTestExt.class)

@PactTestFor(pactVersion = PactSpecVersion.V4)

public class PosOmsConsumerPactTest {

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact loyaltyPact(PactDslWithProvider builder) {
        return builder
                .uponReceiving("Get Loyalty Points")
                .path("/customers/C100/points")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("customerId", "C100")
                        .integerType("points", 2500)
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "loyaltyPact")
    void loyalty(MockServer server) {
        given()
                .baseUri(server.getUrl())
                .when()
                .get("/customers/C100/points")
                .then()
                .statusCode(200)
                .body("points", equalTo(2500));
    }

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact shipmentPact(PactDslWithProvider builder) {
        return builder
                .uponReceiving("Get Shipment")
                .path("/shipment/TRK123")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("trackingId", "TRK123")
                        .stringType("deliveryStatus", "IN_TRANSIT")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "shipmentPact")
    void shipment(MockServer server) {
        given()
                .baseUri(server.getUrl())
                .when()
                .get("/shipment/TRK123")
                .then()
                .statusCode(200)
                .body("deliveryStatus", equalTo("IN_TRANSIT"));
    }

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact paymentPact(PactDslWithProvider builder) {
        return builder
                .uponReceiving("Get Payment")
                .path("/payments/P900")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("paymentId", "P900")
                        .stringType("status", "SUCCESS")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "paymentPact")
    void payment(MockServer server) {
        given()
                .baseUri(server.getUrl())
                .when()
                .get("/payments/P900")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"));
    }
}