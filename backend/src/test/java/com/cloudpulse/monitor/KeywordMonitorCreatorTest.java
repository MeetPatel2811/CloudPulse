package com.cloudpulse.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KeywordMonitorCreatorTest {
    @Test
    void createMonitor_returnsKeywordServiceMonitor() {
        KeywordServiceMonitor keywordServiceMonitor = new KeywordServiceMonitor(null);
        KeywordMonitorCreator creator = new KeywordMonitorCreator(keywordServiceMonitor);
        ServiceMonitor monitor = creator.createMonitor();
        assertInstanceOf(KeywordServiceMonitor.class, monitor);
    }
}
