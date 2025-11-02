package org.the_agora.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FederationConfig {

    @Value("${federation.server-domain}")
    private String serverDomain;

    @Value("${federation.public-endpoint}")
    private String publicEndpoint;

    public String getServerDomain() {
        return serverDomain;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }
}