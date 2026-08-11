package com.cloudpulse.monitor.http;

import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/** Opens a raw TLS handshake (no HTTP request) and reads back the server's leaf certificate. */
@Component
public class JdkTlsCertificateFetcher implements TlsCertificateFetcher {

    private final HttpMonitoringProperties properties;

    public JdkTlsCertificateFetcher(HttpMonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    public X509Certificate fetchLeafCertificate(String host, int port) throws Exception {
        int timeoutMillis = (int) properties.getConnectTimeout().toMillis();
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            socket.startHandshake();

            SSLSession session = socket.getSession();
            Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates.length == 0 || !(peerCertificates[0] instanceof X509Certificate leaf)) {
                throw new IllegalStateException("No X.509 certificate presented by " + host);
            }
            return leaf;
        }
    }
}
