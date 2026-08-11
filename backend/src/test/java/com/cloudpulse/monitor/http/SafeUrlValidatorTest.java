package com.cloudpulse.monitor.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeUrlValidatorTest {

    @Test
    void acceptsPublicHttpAddress() {
        SafeUrlValidator validator = validatorWithAllowedHosts();

        URI uri = validator.validate("https://8.8.8.8/health");

        assertEquals("8.8.8.8", uri.getHost());
    }

    @Test
    void rejectsUnsupportedScheme() {
        SafeUrlValidator validator = validatorWithAllowedHosts();

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("file:///etc/passwd"));
    }

    @Test
    void rejectsCredentialsInUrl() {
        SafeUrlValidator validator = validatorWithAllowedHosts();

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://user:secret@8.8.8.8/"));
    }

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        SafeUrlValidator validator = validatorWithAllowedHosts();

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://127.0.0.1/internal"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://10.0.0.1/internal"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://169.254.169.254/latest/meta-data"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://[fd00::1]/internal"));
    }

    @Test
    void exactProfileHostCanBeAllowed() {
        SafeUrlValidator validator = validatorWithAllowedHosts("demo-standard");

        URI uri = validator.validate("http://demo-standard:8081/health");

        assertEquals("demo-standard", uri.getHost());
    }

    private SafeUrlValidator validatorWithAllowedHosts(String... hosts) {
        HttpMonitoringProperties properties = new HttpMonitoringProperties();
        properties.setAllowedHosts(List.of(hosts));
        return new SafeUrlValidator(properties);
    }
}
