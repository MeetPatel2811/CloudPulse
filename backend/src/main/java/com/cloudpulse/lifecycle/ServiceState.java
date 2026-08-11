package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;

public interface ServiceState {

    ServiceStatus status();

    ServiceStatus transition(ServiceStatus observedStatus);
}
