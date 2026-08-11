package com.cloudpulse.handler;

import com.cloudpulse.model.HealthCheckResult;

// Base class for the Chain of Responsibility.
// Each handler does one step of processing a HealthCheckResult and then
// passes it to the next handler in the chain (or stops).
public abstract class ResultHandler {

    // The next handler in the chain, or null if this is the last one.
    protected ResultHandler next;

    // Links the next handler and returns it, so calls can be chained like
    // a.setNext(b).setNext(c).
    public ResultHandler setNext(ResultHandler next) {
        this.next = next;
        return next;
    }

    // Each handler puts its own step here.
    public abstract void handle(HealthCheckResult result);

    // Passes the result to the next handler if there is one.
    protected void passToNext(HealthCheckResult result) {
        if (next != null) {
            next.handle(result);
        }
    }
}
