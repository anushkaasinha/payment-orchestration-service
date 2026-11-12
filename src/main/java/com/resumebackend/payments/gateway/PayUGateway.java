package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import org.springframework.stereotype.Component;

@Component
public class PayUGateway extends SimulatedGateway {
    public PayUGateway(GatewayRegistry registry) { super(GatewayName.PAYU, "payu_", registry); }
}
