package com.ilterkavlak.HubSpotOutreachOps.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Prospect implements OutreachEntity {
    private final String type = "prospect";
    private Map<String, Object> attributes;
    private Long id;

    public String getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public Prospect setId(Long id) {
        this.id = id;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Prospect setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public String firstEmail() {
        if (attributes != null && attributes.containsKey("emails")) {
            List<String> emails = ((List<String>) attributes.get("emails"));
            if (emails.size() > 0) {
                return emails.get(0);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Prospect{" +
                "type='" + type + '\'' +
                ", attributes=" + attributes +
                ", id=" + id +
                '}';
    }
}
