package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import org.springframework.stereotype.Component;

@Component
public class StripeGateway extends SimulatedGateway {
    public StripeGateway(GatewayRegistry registry) { super(GatewayName.STRIPE, "pi_", registry); }
}
