package com.acadezer.api.ratelimit;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;

class ClientIpResolverTests {
    private MockHttpServletRequest request(String peer, String forwarded) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
        request.addHeader("Forwarded", "for=1.1.1.1");
        request.addHeader("X-Real-IP", "1.1.1.1");
        request.addHeader("CF-Connecting-IP", "1.1.1.1");
        return request;
    }

    @Test void ignoresAllHeadersWithoutConfiguredTrustedPeer() {
        assertThat(new ClientIpResolver("").resolve(request("198.51.100.10", "1.1.1.1"))).isEqualTo("198.51.100.10");
        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request("198.51.100.10", "1.1.1.1"))).isEqualTo("198.51.100.10");
    }

    @Test void walksFromTrustedSocketToFirstUntrustedHopIgnoringForgedPrefix() {
        var resolver = new ClientIpResolver("10.0.0.0/8,192.0.2.0/24");
        assertThat(resolver.resolve(request("10.0.0.1", "1.1.1.1,198.51.100.10,192.0.2.5"))).isEqualTo("198.51.100.10");
        assertThat(resolver.resolve(request("10.0.0.1", "1.1.1.1,203.0.113.5"))).isEqualTo("203.0.113.5");
    }

    @Test void combinesRepeatedHeadersInOrder() {
        var request = request("10.0.0.1", "1.1.1.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10,10.0.0.2");
        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request)).isEqualTo("198.51.100.10");
    }

    @Test void malformedOrOversizedChainsFallBackToSocketWithoutDnsLookups() {
        var resolver = new ClientIpResolver("10.0.0.0/8");
        for (String forwarded : new String[]{"attacker.example", "999.1.1.1", "127.1", "198.51.100.10,", "1.1.1.1,".repeat(300)}) {
            assertThat(resolver.resolve(request("10.0.0.1", forwarded))).isEqualTo("10.0.0.1");
        }
        assertThat(resolver.resolve(request("10.0.0.1", null))).isEqualTo("10.0.0.1");
    }

    @Test void ipv6AddressesAreCanonicalized() throws Exception {
        var resolver = new ClientIpResolver("2001:db8:1::/48");
        assertThat(resolver.resolve(request("2001:db8:1::1", "2001:db8:2::10")))
            .isEqualTo(InetAddress.getByName("2001:db8:2::10").getHostAddress());
    }

    @Test void refusesWildcardNetworksAndHostnamesInTrustedConfiguration() {
        for (String cidr : new String[]{"0.0.0.0/0", "::/0", "proxy.example", "10.0.0.1/33"}) {
            assertThatThrownBy(() -> new ClientIpResolver(cidr)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
