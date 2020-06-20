package com.ilterkavlak.HubSpotClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilterkavlak.HubSpotClient.Entity.ContactList;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HubSpotAdapter {
    public static final String HTTPS = "https";
    public static final String API_HUBAPI_COM = "api.hubapi.com";
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String accessToken;

    private static final int MAX_RETRY = 5;

    public HubSpotAdapter(String refreshKey, String clientId, String clientSecret) {
        this.refreshToken = refreshKey;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public ContactList listRecentlyModifiedContacts(ListContactsRequest request) throws Exception {
        return (ContactList) executeGet(generateContactUrl(Endpoint.RECENTLY_MODIFIED_CONTACTS, request), ContactList.class);
    }

    public <T> Object executeGet(String url, Class<T> object) throws Exception {
        generateAccessTokenIfNecessary();

        HttpGet request = new HttpGet(url);
        request.addHeader(new BasicHeader("Authorization", String.format("Bearer %s", accessToken)));

        int retryCount = 0;
        Exception latestException = null;

        while (retryCount < MAX_RETRY) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Failed to retrieve access token with status line:" + response.getStatusLine().toString());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);

                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(result, object);
                }
            } catch (Exception ex) {
                retryCount++;
                latestException = ex;
            }
        }
        throw latestException;
    }

    protected static String generateContactUrl(Endpoint endpoint, ListContactsRequest request) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(HTTPS);
        uriBuilder.setHost(API_HUBAPI_COM);
        uriBuilder.setPath(endpoint.getEndpoint());

        if (request.getContactOffset() != 0L) {
            uriBuilder.addParameter("vidOffset", String.valueOf(request.getContactOffset()));
        }
        if (request.getCount() != 0) {
            uriBuilder.addParameter("count", String.valueOf(request.getCount()));
        }
        if (request.getTimeOffset() != 0L) {
            uriBuilder.addParameter("timeOffset", String.valueOf(request.getTimeOffset()));
        }
        for (String property : request.getPropertyList()) {
            uriBuilder.addParameter("property", property);
        }

        return uriBuilder.build().toString();
    }

    private void generateAccessTokenIfNecessary() throws Exception {
        if (!StringUtils.isEmpty(accessToken)) {
            return;
        }

        HttpPost request = new HttpPost("https://api.hubapi.com/oauth/v1/token");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
        nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
        nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
        nameValuePairs.add(new BasicNameValuePair("refresh_token", refreshToken));
        request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        int retryCount = 0;
        Exception latestException = null;

        while (retryCount < MAX_RETRY) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Failed to retrieve access token with status line:" + response.getStatusLine().toString());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);

                    ObjectMapper objectMapper = new ObjectMapper();
                    this.accessToken = String.valueOf(objectMapper.readValue(result, Map.class).get("access_token"));
                    return;
                }
            } catch (Exception ex) {
                retryCount++;
                latestException = ex;
            }
        }
        throw latestException;
    }
}