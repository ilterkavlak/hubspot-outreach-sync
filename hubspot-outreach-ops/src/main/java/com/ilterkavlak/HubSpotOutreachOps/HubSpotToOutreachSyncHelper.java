package com.ilterkavlak.HubSpotOutreachOps;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.ilterkavlak.HubSpotClient.Entity.Contact;
import com.ilterkavlak.HubSpotClient.Entity.ContactList;
import com.ilterkavlak.HubSpotClient.HubSpotAdapter;
import com.ilterkavlak.HubSpotClient.ListContactsRequest;
import com.ilterkavlak.HubSpotOutreachOps.Entity.Prospect;
import com.ilterkavlak.HubSpotOutreachOps.credentials.HubSpotCredentials;
import com.ilterkavlak.HubSpotOutreachOps.credentials.OutreachCredentials;

import java.util.*;

import static com.ilterkavlak.HubSpotClient.Entity.Contact.HUBSPOT_CONTACT_PROPERTY_VALUE_KEY;

public class HubSpotToOutreachSyncHelper {

    private LambdaLogger LOGGER;
    public static final int HUBSPOT_CONTACT_COUNT = 100;
    private HubSpotAdapter hubSpotAdapter;
    private OutreachAdapter outreachAdapter;
    private Lookup lookup;

    private static HubSpotToOutreachSyncHelper hubSpotToOutreachSync;

    private HubSpotToOutreachSyncHelper() {
    }

    public static HubSpotToOutreachSyncHelper init(HubSpotCredentials hubspotCredentials, OutreachCredentials outreachCredentials, Lookup lookup, LambdaLogger logger) {
        if (hubSpotToOutreachSync == null) {
            hubSpotToOutreachSync = new HubSpotToOutreachSyncHelper();
            hubSpotToOutreachSync.hubSpotAdapter = new HubSpotAdapter(hubspotCredentials.getRefreshKey(), hubspotCredentials.getClientId(), hubspotCredentials.getClientSecret());
            hubSpotToOutreachSync.outreachAdapter = new OutreachAdapter(outreachCredentials.getRefreshKey(), outreachCredentials.getClientId(), outreachCredentials.getClientSecret(), outreachCredentials.getRedirectUri());
            hubSpotToOutreachSync.lookup = lookup;
            hubSpotToOutreachSync.LOGGER = logger;
        }
        return hubSpotToOutreachSync;
    }

    public void syncLatestHubSpotContactsToOutreach(Map<String, String> hubSpotOutreachFieldMapping) throws Exception {
        List<String> hubspotContactPropertyKeyList = new ArrayList<>(hubSpotOutreachFieldMapping.keySet());

        long lastProcessedTimestamp = lookup.getLastProcessedTimestamp();
        long maxUpdatedTimeStamp = lastProcessedTimestamp;
        long timeOffset;
        boolean hasMore;
        long vidOffset;

        LOGGER.log("Starting sync procedure.");
        ContactList contactList = hubSpotAdapter.listRecentlyModifiedContacts(new ListContactsRequest().setPropertyList(hubspotContactPropertyKeyList).setCount(HUBSPOT_CONTACT_COUNT));
        do {
            hasMore = contactList.isHasMore();
            timeOffset = contactList.getTimeOffset();
            vidOffset = contactList.getVidOffset();
            LOGGER.log(String.format("Got %d contacts with hasMore: %b, timeOffset: %d, vidOffset: %d", contactList.getContacts().size(), hasMore, timeOffset, vidOffset));
            for (Contact contact : contactList.getContacts()) {
                long contactLastModifiedTimestamp = contact.lastModifiedTimestamp();
                if (contactLastModifiedTimestamp < lastProcessedTimestamp) {
                    hasMore = false;
                    continue;
                }
                Map<String, Object> prospectAttributes = new HashMap<>();
                hubSpotOutreachFieldMapping.forEach((key, value) -> {
                    if (contact.getProperties().containsKey(key)) {
                        String fieldValue = contact.getProperties().get(key).get(HUBSPOT_CONTACT_PROPERTY_VALUE_KEY);
                        prospectAttributes.put(value, key.equals("email") ? Collections.singletonList(fieldValue) : fieldValue);
                    }
                });
                Prospect prospect = new Prospect().setAttributes(prospectAttributes);
                Prospect prospectOnOutreach = outreachAdapter.createOrUpdateProspect(prospect);
                if (prospectOnOutreach != null) {
                    LOGGER.log(String.format("Synced prospect with id: %s, properties: %s", prospectOnOutreach.getId(), prospectOnOutreach.getAttributes()));
                    maxUpdatedTimeStamp = Math.max(maxUpdatedTimeStamp, contactLastModifiedTimestamp);
                }
            }
            contactList = hubSpotAdapter.listRecentlyModifiedContacts(new ListContactsRequest().setTimeOffset(timeOffset).setContactOffset(vidOffset).setPropertyList(hubspotContactPropertyKeyList).setCount(HUBSPOT_CONTACT_COUNT));
        } while (hasMore);
        lookup.setLastProcessedTimestamp(maxUpdatedTimeStamp);
    }
}
