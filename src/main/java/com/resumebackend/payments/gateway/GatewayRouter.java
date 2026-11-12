package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.domain.GatewayOutcome;
import com.resumebackend.payments.gateway.PaymentGateway.ChargeRequest;
import com.resumebackend.payments.gateway.PaymentGateway.GatewayResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Routes a charge across gateways in priority order.
 *  - transient failure (outage/timeout) -> fail over to the next gateway
 *  - hard failure (decline)             -> stop; another gateway will not change the answer
 *  - circuit open                       -> skip the gateway without calling it
 */
@Service
public class GatewayRouter {
    public record Attempt(GatewayName gateway, GatewayResult result) {}
    public record RouteResult(List<Attempt> attempts, Attempt success, boolean hardFailure) {}

    private final Map<GatewayName, PaymentGateway> gateways = new EnumMap<>(GatewayName.class);
    private final List<GatewayName> order;
    private final GatewayRegistry registry;

    public GatewayRouter(List<PaymentGateway> all, GatewayRegistry registry,
                         @Value("${payments.gateways.priority}") String priority) {
        all.forEach(g -> gateways.put(g.name(), g));
        this.registry = registry;
        this.order = Arrays.stream(priority.split(",")).map(String::trim).map(GatewayName::valueOf).toList();
    }

    public RouteResult route(ChargeRequest request) {
        List<Attempt> attempts = new ArrayList<>();
        for (GatewayName g : order) {
            if (registry.isOpen(g)) {
                attempts.add(new Attempt(g, new GatewayResult(GatewayOutcome.SKIPPED, null, "CIRCUIT_OPEN",
                        g + ": circuit breaker open, skipped", 0)));
                continue;
            }
            GatewayResult res = gateways.get(g).charge(request);
            Attempt a = new Attempt(g, res);
            attempts.add(a);
            switch (res.outcome()) {
                case SUCCESS -> { registry.recordSuccess(g); return new RouteResult(attempts, a, false); }
                case HARD_FAILURE -> { registry.recordSuccess(g); return new RouteResult(attempts, null, true); }
                default -> registry.recordFailure(g);
            }
        }
        return new RouteResult(attempts, null, false);
    }
}
