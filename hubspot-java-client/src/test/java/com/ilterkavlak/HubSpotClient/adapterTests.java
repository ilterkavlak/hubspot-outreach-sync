package com.ilterkavlak.HubSpotClient;

import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

class adapterTests {

    @Test
    void generateContactUrl() throws URISyntaxException {
        ListContactsRequest request = new ListContactsRequest()
                .setCount(100)
                .setContactOffset(1234L)
                .setTimeOffset(4321L)
                .setPropertyList(Arrays.asList("firstname", "lastname", "company"));
        assertEquals("https://api.hubapi.com/contacts/v1/lists/all/contacts/recent?vidOffset=1234&count=100&timeOffset=4321&property=firstname&property=lastname&property=company", HubSpotAdapter.generateContactUrl(Endpoint.RECENTLY_CREATED_CONTACTS, request));
    }

}
