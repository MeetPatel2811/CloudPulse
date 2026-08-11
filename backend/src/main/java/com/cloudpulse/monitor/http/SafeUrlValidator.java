package com.cloudpulse.monitor.http;

import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rejects targets that could turn public URL monitoring into access to the host's
 * private network. Exact profile-specific demo hosts can be allowlisted explicitly.
 */
@Component
public class SafeUrlValidator {

    private final Set<String> allowedHosts;

    public SafeUrlValidator(HttpMonitoringProperties properties) {
        this.allowedHosts = properties.getAllowedHosts().stream()
                .map(SafeUrlValidator::normalizeHost)
                .collect(Collectors.toUnmodifiableSet());
    }

    public URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }

        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("URL is malformed", exception);
        }
        return validate(uri);
    }

    public URI validate(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("URL must use http or https");
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("URL must not contain credentials");
        }

        String host = normalizeHost(uri.getHost());
        if (host.isBlank()) {
            throw new IllegalArgumentException("URL must include a valid host");
        }
        if (allowedHosts.contains(host)) {
            return uri;
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("URL host could not be resolved", exception);
        }

        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new IllegalArgumentException("URL resolves to a private or restricted address");
            }
        }
        return uri;
    }

    private boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            // Shared carrier space and network benchmarking ranges are not public targets.
            return (first == 100 && second >= 64 && second <= 127)
                    || (first == 198 && (second == 18 || second == 19));
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            // fc00::/7 — IPv6 unique-local addresses.
            return (first & 0xFE) == 0xFC;
        }
        return true;
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
}
