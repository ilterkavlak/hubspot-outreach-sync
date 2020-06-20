package com.ilterkavlak.HubSpotClient.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactList {

    private List<Contact> contacts;
    @JsonProperty("has-more")
    private boolean hasMore;
    @JsonProperty("vid-offset")
    private long vidOffset;
    @JsonProperty("time-offset")
    private long timeOffset;

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public ContactList setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
        return this;
    }

    public long getVidOffset() {
        return vidOffset;
    }

    public ContactList setVidOffset(long vidOffset) {
        this.vidOffset = vidOffset;
        return this;
    }

    public long getTimeOffset() {
        return timeOffset;
    }

    public ContactList setTimeOffset(long timeOffset) {
        this.timeOffset = timeOffset;
        return this;
    }

    @Override
    public String toString() {
        return "ContactList{" +
                "contacts=" + contacts +
                ", hasMore=" + hasMore +
                ", vidOffset=" + vidOffset +
                ", timeOffset=" + timeOffset +
                '}';
    }
}
