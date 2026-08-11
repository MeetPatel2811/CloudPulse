package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class RecoveredState implements ServiceState {

    @Override
    public ServiceStatus status() {
        return ServiceStatus.RECOVERED;
    }

    @Override
    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus == ServiceStatus.HEALTHY
                ? ServiceStatus.HEALTHY
                : observedStatus;
    }
}
