package com.ust.sdet.api;

import au.com.dius.pact.provider.junit5.*;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("OMS")
@PactFolder("target/pacts")

public class OmsProviderPactTest {

    @BeforeAll
    static void start() throws Exception {
        FakeOmsServer.start();
    }

    @AfterAll
    static void stop() {
        FakeOmsServer.stop();
    }

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(
                new HttpTestTarget("localhost", 4010)
        );
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)

    void verify(
            PactVerificationContext context
    ) {

        context.verifyInteraction();

    }

}