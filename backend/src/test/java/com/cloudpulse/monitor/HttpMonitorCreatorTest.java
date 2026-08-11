package com.cloudpulse.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HttpMonitorCreatorTest {
    @Test
    void createMonitor_returnsHttpServiceMonitor() {
        HttpServiceMonitor httpServiceMonitor = new HttpServiceMonitor(null);
        HttpMonitorCreator creator = new HttpMonitorCreator(httpServiceMonitor);
        ServiceMonitor monitor = creator.createMonitor();
        assertInstanceOf(HttpServiceMonitor.class, monitor);
    }
}
