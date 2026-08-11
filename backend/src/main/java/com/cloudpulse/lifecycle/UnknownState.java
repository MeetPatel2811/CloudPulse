package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class UnknownState implements ServiceState {

    @Override
    public ServiceStatus status() {
        return ServiceStatus.UNKNOWN;
    }

    @Override
    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus;
    }
}
