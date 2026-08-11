package com.cloudpulse.command;

// Command pattern: every action in CloudPulse (run a check, acknowledge or
// resolve an alert, enable/disable monitoring) implements this one method.
public interface MonitoringCommand {
    void execute();
}
