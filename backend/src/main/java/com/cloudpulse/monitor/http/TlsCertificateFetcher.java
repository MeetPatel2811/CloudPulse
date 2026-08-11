package com.cloudpulse.monitor.http;

import java.security.cert.X509Certificate;

/** Small transport boundary for the TLS handshake, kept independently testable. */
public interface TlsCertificateFetcher {
    X509Certificate fetchLeafCertificate(String host, int port) throws Exception;
}
