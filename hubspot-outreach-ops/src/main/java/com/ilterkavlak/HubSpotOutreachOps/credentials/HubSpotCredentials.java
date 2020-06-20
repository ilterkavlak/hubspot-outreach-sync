package com.ilterkavlak.HubSpotOutreachOps.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HubSpotCredentials {
    private String refreshKey;
    private String clientId;
    private String clientSecret;

    public String getRefreshKey() {
        return refreshKey;
    }

    public HubSpotCredentials setRefreshKey(String refreshKey) {
        this.refreshKey = refreshKey;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public HubSpotCredentials setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public HubSpotCredentials setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public String toString() {
        return "HubSpotCredentials{" +
                "refreshKey='" + refreshKey + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                '}';
    }
}
