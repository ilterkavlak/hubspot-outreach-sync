package com.ilterkavlak.HubSpotOutreachOps;

import java.io.IOException;

public interface Lookup {

    long getLastProcessedTimestamp() throws IOException;

    void setLastProcessedTimestamp(long lastProcessedTimestamp);
}
