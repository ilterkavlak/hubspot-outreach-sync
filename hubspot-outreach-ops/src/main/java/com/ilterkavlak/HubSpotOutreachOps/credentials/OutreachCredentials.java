package com.ilterkavlak.HubSpotOutreachOps.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutreachCredentials {
    private String refreshKey;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public String getRefreshKey() {
        return refreshKey;
    }

    public OutreachCredentials setRefreshKey(String refreshKey) {
        this.refreshKey = refreshKey;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public OutreachCredentials setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public OutreachCredentials setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public OutreachCredentials setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    @Override
    public String toString() {
        return "OutreachCredentials{" +
                "refreshKey='" + refreshKey + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                '}';
    }
}
