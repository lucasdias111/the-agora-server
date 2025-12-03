package org.margin.server.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FederationConfig {

    @Value("${federation.server-domain}")
    private String serverDomain;

    @Value("${federation.public-endpoint}")
    private String publicEndpoint;

}