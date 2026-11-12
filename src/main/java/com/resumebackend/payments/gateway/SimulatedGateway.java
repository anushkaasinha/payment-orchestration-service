package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.domain.GatewayOutcome;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Simulates provider behaviour: success, transient outage (failover-worthy), or hard decline (not retried elsewhere). */
public abstract class SimulatedGateway implements PaymentGateway {
    private final GatewayName name;
    private final String prefix;
    private final GatewayRegistry registry;

    protected SimulatedGateway(GatewayName name, String prefix, GatewayRegistry registry) {
        this.name = name;
        this.prefix = prefix;
        this.registry = registry;
    }

    @Override public GatewayName name() { return name; }

    @Override
    public GatewayResult charge(ChargeRequest request) {
        long start = System.nanoTime();
        try { Thread.sleep(ThreadLocalRandom.current().nextLong(30, 120)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long ms = (System.nanoTime() - start) / 1_000_000;

        GatewayMode mode = registry.mode(name);
        boolean flakyFail = mode == GatewayMode.FLAKY && ThreadLocalRandom.current().nextDouble() < 0.6;
        return switch (mode) {
            case DOWN -> transientFailure(ms);
            case DECLINE -> new GatewayResult(GatewayOutcome.HARD_FAILURE, null, "CARD_DECLINED",
                    name + ": card declined by issuer", ms);
            case FLAKY -> flakyFail ? transientFailure(ms) : success(ms);
            case UP -> success(ms);
        };
    }

    private GatewayResult success(long ms) {
        return new GatewayResult(GatewayOutcome.SUCCESS, prefix + UUID.randomUUID().toString().substring(0, 12),
                "OK", name + ": captured", ms);
    }

    private GatewayResult transientFailure(long ms) {
        return new GatewayResult(GatewayOutcome.TRANSIENT_FAILURE, null, "GATEWAY_UNAVAILABLE",
                name + ": 503 service unavailable / timeout", ms);
    }
}
