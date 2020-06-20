package com.ilterkavlak.HubSpotClient;

public enum Endpoint {
    RECENTLY_MODIFIED_CONTACTS("/contacts/v1/lists/recently_updated/contacts/recent"),
    RECENTLY_CREATED_CONTACTS("/contacts/v1/lists/all/contacts/recent");

    private String endpoint;

    Endpoint(String endpoint){
        this.endpoint = endpoint;
    }

    public String getEndpoint(){
        return endpoint;
    }
}
