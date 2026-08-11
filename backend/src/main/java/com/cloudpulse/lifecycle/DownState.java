package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class DownState implements ServiceState {

    @Override
    public ServiceStatus status() {
        return ServiceStatus.DOWN;
    }

    @Override
    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus == ServiceStatus.HEALTHY
                ? ServiceStatus.RECOVERED
                : observedStatus;
    }
}
