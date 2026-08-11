package com.cloudpulse.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MockMonitorCreatorTest {
    @Test
    void createMonitor_returnsMockServiceMonitor() {
        MockMonitorCreator creator = new MockMonitorCreator();
        ServiceMonitor monitor = creator.createMonitor();
        assertInstanceOf(MockServiceMonitor.class, monitor);
    }
}