package com.cloudpulse.lifecycle;

import com.cloudpulse.model.ServiceStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ServiceStateMachine {

    private final Map<ServiceStatus, ServiceState> states;

    public ServiceStateMachine(List<ServiceState> states) {
        this.states = new EnumMap<>(ServiceStatus.class);
        states.forEach(state -> this.states.put(state.status(), state));
    }

    public ServiceStatus transition(
            ServiceStatus currentStatus,
            ServiceStatus observedStatus
    ) {
        ServiceState currentState = states.get(currentStatus);
        if (currentState == null) {
            throw new IllegalArgumentException("No state registered for " + currentStatus);
        }
        return currentState.transition(observedStatus);
    }
}
