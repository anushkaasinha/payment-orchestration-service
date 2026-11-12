package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Holds simulated gateway modes (for demos/tests) and circuit-breaker state per gateway. */
@Component
public class GatewayRegistry {
    private final Map<GatewayName, GatewayMode> modes = new ConcurrentHashMap<>();
    private final Map<GatewayName, Integer> failures = new ConcurrentHashMap<>();
    private final Map<GatewayName, Instant> openUntil = new ConcurrentHashMap<>();
    private final int threshold;
    private final long openSeconds;

    public GatewayRegistry(@Value("${payments.breaker.failure-threshold:3}") int threshold,
                           @Value("${payments.breaker.open-seconds:30}") long openSeconds) {
        this.threshold = threshold;
        this.openSeconds = openSeconds;
    }

    public GatewayMode mode(GatewayName g) { return modes.getOrDefault(g, GatewayMode.UP); }

    public void setMode(GatewayName g, GatewayMode m) {
        modes.put(g, m);
        failures.remove(g);
        openUntil.remove(g);
    }

    public boolean isOpen(GatewayName g) {
        Instant t = openUntil.get(g);
        return t != null && Instant.now().isBefore(t);
    }

    public void recordSuccess(GatewayName g) { failures.remove(g); openUntil.remove(g); }

    public void recordFailure(GatewayName g) {
        int n = failures.merge(g, 1, Integer::sum);
        if (n >= threshold) {
            openUntil.put(g, Instant.now().plusSeconds(openSeconds));
            failures.remove(g);
        }
    }

    public void reset() { modes.clear(); failures.clear(); openUntil.clear(); }

    public List<Map<String, Object>> snapshot() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GatewayName g : GatewayName.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gateway", g);
            m.put("mode", mode(g));
            m.put("circuitOpen", isOpen(g));
            m.put("circuitOpenUntil", openUntil.get(g));
            out.add(m);
        }
        return out;
    }
}
