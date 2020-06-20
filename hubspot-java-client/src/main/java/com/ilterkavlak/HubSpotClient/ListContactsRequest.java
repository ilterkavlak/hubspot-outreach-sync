package com.ilterkavlak.HubSpotClient;

import java.util.ArrayList;
import java.util.List;

public class ListContactsRequest {

    private int count;
    private long timeOffset;
    private long contactOffset;
    private List<String> propertyList = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public ListContactsRequest setCount(int count) {
        this.count = count;
        return this;
    }

    public long getTimeOffset() {
        return timeOffset;
    }

    public ListContactsRequest setTimeOffset(long timeOffset) {
        this.timeOffset = timeOffset;
        return this;
    }

    public long getContactOffset() {
        return contactOffset;
    }

    public ListContactsRequest setContactOffset(long contactOffset) {
        this.contactOffset = contactOffset;
        return this;
    }

    public List<String> getPropertyList() {
        return propertyList;
    }

    public ListContactsRequest setPropertyList(List<String> propertyList) {
        this.propertyList = propertyList;
        return this;
    }

    @Override
    public String toString() {
        return "ListRecentlyModifiedContactsRequest{" +
                "count=" + count +
                ", timeOffset=" + timeOffset +
                ", contactOffset=" + contactOffset +
                ", propertyList=" + propertyList +
                '}';
    }
}
