package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class HealthyState implements ServiceState {

    @Override
    public ServiceStatus status() {
        return ServiceStatus.HEALTHY;
    }

    @Override
    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus;
    }
}
