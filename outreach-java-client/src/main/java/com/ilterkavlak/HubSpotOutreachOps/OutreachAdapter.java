package com.ilterkavlak.HubSpotOutreachOps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilterkavlak.HubSpotOutreachOps.Entity.OutreachEntity;
import com.ilterkavlak.HubSpotOutreachOps.Entity.Prospect;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class OutreachAdapter {
    public static final String HTTPS = "https";
    public static final String API_OUTREACH_IO = "api.outreach.io";
    public static final String APPLICATION_JSON = "application/json";

    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private String redirectUri;

    public static final long RETRY_WAIT_TIME = TimeUnit.SECONDS.toMillis(0);
    private static final int MAX_RETRY = 5;

    public OutreachAdapter(String refreshKey, String clientId, String clientSecret, String redirectUri) {
        this.refreshToken = refreshKey;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public Prospect createOrUpdateProspect(Prospect prospect) throws Exception {
        try {
            return executePost(generateCreateProspectUrl(Endpoint.CREATE_PROSPECT), prospect, Prospect.class);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("Contacts email hash has already been taken")) {
                List<Prospect> retreivedProspectList = executeGet(generateGetProspectURI(Endpoint.CREATE_PROSPECT, prospect), Prospect.class);
                if (retreivedProspectList.size() > 1) {
                    throw new IllegalStateException("There are more than 1 prospects with email: " + prospect.firstEmail());
                }
                Prospect retrievedProspect = retreivedProspectList.get(0);
                return executePatch(generateUpdateProspectURI(Endpoint.CREATE_PROSPECT, retrievedProspect), prospect.setId(retrievedProspect.getId()), Prospect.class);
            } else if (ex.getMessage().contains("Contacts contact is using an excluded email address")) {
                System.out.println(String.format("Skipping contact creation for %s, with message: %s", new ObjectMapper().writeValueAsString(prospect), ex.getMessage()));
                return null;
            } else {
                throw ex;
            }
        }
    }

    private String generateCreateProspectUrl(Endpoint endpoint) throws URISyntaxException {
        return generateBaseUri(endpoint).build().toString();
    }

    private URIBuilder generateBaseUri(Endpoint endpoint) {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(HTTPS);
        uriBuilder.setHost(API_OUTREACH_IO);
        uriBuilder.setPath(endpoint.getEndpoint());
        return uriBuilder;
    }

    private String generateGetProspectURI(Endpoint endpoint, Prospect prospect) throws URISyntaxException {
        return generateBaseUri(endpoint).addParameter("filter[emails]", prospect.firstEmail()).build().toString();
    }

    private String generateUpdateProspectURI(Endpoint endpoint, Prospect prospect) throws URISyntaxException {
        URIBuilder uriBuilder = generateBaseUri(endpoint);
        return uriBuilder.setPath(String.format("%s/%d", uriBuilder.getPath(), prospect.getId())).build().toString();
    }

    private <T> T executePost(String url, OutreachEntity object, Class<T> returnType) throws Exception {
        generateAccessTokenIfNecessary();

        ObjectMapper objectMapper = new ObjectMapper();

        HttpPost request = new HttpPost(url);
        populateRequestForUpdate(object, objectMapper, request);

        return executeInRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                handleErrorsInUpdateRequests(response);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    Map<String, Object> resultObjectMap = (Map<String, Object>) objectMapper.readValue(result, Map.class).get("data");
                    return objectMapper.convertValue(resultObjectMap, returnType);
                }
            }
            return null;
        });
    }

    private void populateRequestForUpdate(OutreachEntity object, ObjectMapper objectMapper, HttpEntityEnclosingRequestBase request) throws JsonProcessingException {
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)));
        request.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);

        Map<String, Object> entityObject = new HashMap<>();
        entityObject.put("data", object);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(entityObject), ContentType.APPLICATION_JSON));
    }

    private void generateAccessTokenIfNecessary() throws Exception {
        if (StringUtils.isNotEmpty(accessToken)) {
            return;
        }

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(HTTPS);
        uriBuilder.setHost(API_OUTREACH_IO);
        uriBuilder.setPath(Endpoint.OAUTH_TOKEN.getEndpoint());

        HttpPost request = new HttpPost(uriBuilder.build().toString());
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
        nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
        nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
        nameValuePairs.add(new BasicNameValuePair("refresh_token", refreshToken));
        nameValuePairs.add(new BasicNameValuePair("redirect_uri", redirectUri));
        request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        executeInRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Failed to retrieve access token with status line:" + response.getStatusLine().toString());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);

                    ObjectMapper objectMapper = new ObjectMapper();
                    this.accessToken = String.valueOf(objectMapper.readValue(result, Map.class).get("access_token"));
                    return null;
                }
            }
            return null;
        });
    }

    private <T> List<T> executeGet(String url, Class<T> classType) throws Exception {
        generateAccessTokenIfNecessary();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpGet request = new HttpGet(url);
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)));
        request.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);

        return executeInRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
                    throw new IllegalArgumentException("Unprocessable entity exception received:" + EntityUtils.toString(response.getEntity()));
                } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Failed to execute get request with status line:" + response.getStatusLine().toString());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    List<Map<String, Object>> resultList = (List<Map<String, Object>>) objectMapper.readValue(result, Map.class).get("data");

                    ArrayList<T> resultEntityList = new ArrayList<>();
                    for (Map<String, Object> resultMap : resultList) {
                        resultEntityList.add(objectMapper.convertValue(resultMap, classType));
                    }
                    return resultEntityList;
                }
            }
            return null;
        });
    }

    private <T> T executeInRetry(Callable<T> func) throws Exception {
        int retryCount = 0;
        Exception latestException = null;
        while (retryCount < MAX_RETRY) {
            try {
                return func.call();
            } catch (Exception ex) {
                if (ex instanceof IllegalArgumentException && ex.getMessage().contains("Unprocessable entity exception received")) {
                    throw ex;
                }
                retryCount++;
                latestException = ex;
                Thread.sleep(RETRY_WAIT_TIME);
            }
        }
        throw latestException;
    }

    private <T> T executePatch(String url, OutreachEntity object, Class<T> returnType) throws Exception {
        generateAccessTokenIfNecessary();

        ObjectMapper objectMapper = new ObjectMapper();

        HttpPatch request = new HttpPatch(url);
        populateRequestForUpdate(object, objectMapper, request);

        return executeInRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                handleErrorsInUpdateRequests(response);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    Map<String, Object> resultObjectMap = (Map<String, Object>) objectMapper.readValue(result, Map.class).get("data");
                    return objectMapper.convertValue(resultObjectMap, returnType);
                }
            }
            return null;
        });
    }

    private void handleErrorsInUpdateRequests(CloseableHttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
            throw new IllegalArgumentException("Unprocessable entity exception received:" + EntityUtils.toString(response.getEntity()));
        } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IllegalStateException("Failed to execute post request with status line:" + response.getStatusLine().toString());
        }
    }
}