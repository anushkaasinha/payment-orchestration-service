package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import org.springframework.stereotype.Component;

@Component
public class PaytmGateway extends SimulatedGateway {
    public PaytmGateway(GatewayRegistry registry) { super(GatewayName.PAYTM, "ptm_", registry); }
}
