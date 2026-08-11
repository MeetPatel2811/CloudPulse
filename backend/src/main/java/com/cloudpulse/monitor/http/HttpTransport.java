package com.cloudpulse.monitor.http;

import java.io.IOException;
import java.net.URI;

/** Small transport boundary that keeps redirect/security logic independently testable. */
public interface HttpTransport {
    HttpTransportResponse execute(URI uri) throws IOException, InterruptedException;
}
