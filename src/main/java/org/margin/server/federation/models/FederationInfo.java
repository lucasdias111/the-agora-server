package org.margin.server.federation.models;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FederationInfo {
    private String serverDomain;
    private String version;
    private Map<String, String> endpoints;
}