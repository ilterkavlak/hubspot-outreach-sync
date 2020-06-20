package com.ilterkavlak.HubSpotOutreachOps;

public enum Endpoint {
    OAUTH_TOKEN("/oauth/token"),
    CREATE_PROSPECT("/api/v2/prospects");

    private String endpoint;

    Endpoint(String endpoint){
        this.endpoint = endpoint;
    }

    public String getEndpoint(){
        return endpoint;
    }
}
