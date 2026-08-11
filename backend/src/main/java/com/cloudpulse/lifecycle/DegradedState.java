package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class DegradedState implements ServiceState {

    @Override
    public ServiceStatus status() {
        return ServiceStatus.DEGRADED;
    }

    @Override
    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus == ServiceStatus.HEALTHY
                ? ServiceStatus.RECOVERED
                : observedStatus;
    }
}
