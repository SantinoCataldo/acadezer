package com.acadezer.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    private final List<IpAddressMatcher> trusted = new ArrayList<>();

    public ClientIpResolver(@Value("${app.limits.trusted-proxy-cidrs}") String cidrs) {
        for (String entry : cidrs.split(",")) {
            String cidr = entry.trim();
            if (cidr.isEmpty()) continue;
            String[] parts = cidr.split("/", -1);
            if (parts.length > 2 || literal(parts[0]) == null || (parts.length == 2 && Integer.parseInt(parts[1]) <= 0)) {
                throw new IllegalArgumentException("Proxy confiable inválido: se requiere una IP o CIDR específica");
            }
            trusted.add(new IpAddressMatcher(cidr));
        }
    }

    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (!isTrusted(peer)) return peer;

        // Combine repeated headers in wire order; refuse oversized/malformed chains.
        var headers = request.getHeaders("X-Forwarded-For");
        StringBuilder chain = new StringBuilder();
        while (headers != null && headers.hasMoreElements()) {
            String value = headers.nextElement();
            if (chain.length() + value.length() + 1 > 2048) return peer;
            if (!chain.isEmpty()) chain.append(',');
            chain.append(value);
        }
        if (chain.isEmpty()) return peer;
        String[] values = chain.toString().split(",", -1);
        if (values.length > 32) return peer;
        List<String> addresses = new ArrayList<>();
        for (String value : values) {
            String address = literal(value.trim());
            if (address == null) return peer;
            addresses.add(address);
        }
        String current = peer;
        for (int i = addresses.size() - 1; i >= 0 && isTrusted(current); i--) {
            current = addresses.get(i);
        }
        return current;
    }

    private boolean isTrusted(String address) {
        return trusted.stream().anyMatch(matcher -> matcher.matches(address));
    }

    /** Parse only IP literals, never DNS names supplied by a client. */
    private static String literal(String value) {
        if (value.contains(":")) {
            if (!value.matches("[0-9a-fA-F:.]+")) return null;
        } else {
            String[] octets = value.split("\\.", -1);
            if (octets.length != 4) return null;
            for (String octet : octets) {
                if (!octet.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octet) > 255) return null;
            }
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }
}
