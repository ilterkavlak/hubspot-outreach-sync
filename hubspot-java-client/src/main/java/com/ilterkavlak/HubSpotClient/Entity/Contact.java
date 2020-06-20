package com.ilterkavlak.HubSpotClient.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contact {
    public static final String HUBSPOT_PROPERTY_LASTMODIFIEDDATE = "lastmodifieddate";
    public static final String HUBSPOT_CONTACT_PROPERTY_VALUE_KEY = "value";

    private long addedAt;
    private long vid;
    private Map<String, Map<String, String>> properties;

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public long getVid() {
        return vid;
    }

    public void setVid(long vid) {
        this.vid = vid;
    }

    public Map<String, Map<String, String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Map<String, String>> properties) {
        this.properties = properties;
    }

    public long lastModifiedTimestamp() {
        return Long.valueOf(getProperties().get(HUBSPOT_PROPERTY_LASTMODIFIEDDATE).get(HUBSPOT_CONTACT_PROPERTY_VALUE_KEY));
    }

    @Override
    public String toString() {
        return "Contact{" +
                "addedAt=" + addedAt +
                ", vid=" + vid +
                ", properties=" + properties +
                '}';
    }
}
