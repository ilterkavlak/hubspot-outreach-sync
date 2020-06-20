package com.ilterkavlak.HubSpotOutreachOps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilterkavlak.HubSpotOutreachOps.credentials.HubSpotCredentials;
import com.ilterkavlak.HubSpotOutreachOps.credentials.OutreachCredentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class LambdaFunction implements RequestHandler<Map<String, Object>, String> {
    private LambdaLogger LOGGER;

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        LOGGER = context.getLogger();

        String lookupBucketName = System.getenv("LOOKUP_BUCKET_NAME");
        String lookupKey = System.getenv("LOOKUP_KEY");
        String credentialsBucketName = System.getenv("CREDENTIALS_BUCKET_NAME");
        String hubSpotCredentialsObjectKey = System.getenv("HUBSPOT_CREDENTIALS_OBJECT_KEY");
        String outreachCredentialsObjectKey = System.getenv("OUTREACH_CREDENTIALS_OBJECT_KEY");
        String hubSpotOutreachFieldMappingObjectKey = System.getenv("HUBSPOT_OUTREACH_FIELD_MAPPING_OBJECT_KEY");

        HubSpotCredentials hubSpotCredentials;
        OutreachCredentials outreachCredentials;
        Map<String, String> hubSpotOutreachFieldMapping;

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

        try {
            hubSpotCredentials = getHubSpotCredentials(s3Client, credentialsBucketName, hubSpotCredentialsObjectKey);
            outreachCredentials = getOutreachCredentials(s3Client, credentialsBucketName, outreachCredentialsObjectKey);
            hubSpotOutreachFieldMapping = getHubSpotOutreachFieldMapping(s3Client, lookupBucketName, hubSpotOutreachFieldMappingObjectKey);
        } catch (IOException e) {
            throw new RuntimeException("Could not get credentials");
        }

        HubSpotToOutreachSyncHelper hubSpotToOutreachSyncHelper = HubSpotToOutreachSyncHelper.init(
                hubSpotCredentials,
                outreachCredentials,
                new S3Lookup(s3Client, lookupBucketName, lookupKey, LOGGER),
                LOGGER
        );

        try {
            hubSpotToOutreachSyncHelper.syncLatestHubSpotContactsToOutreach(hubSpotOutreachFieldMapping);
        } catch (Exception e) {
            throw new RuntimeException("Could not sync contact", e);
        }

        return "200 OK";
    }

    private HubSpotCredentials getHubSpotCredentials(AmazonS3 s3Client, String credentialsBucketName, String hubSpotCredentialsObjectKey) throws IOException {
        LOGGER.log("Getting HubSpot credentials...");
        S3Object s3Object = s3Client.getObject(new GetObjectRequest(credentialsBucketName, hubSpotCredentialsObjectKey));
        if (s3Object == null) {
            throw new IllegalArgumentException(String.format("Could not get object with bucket: %s, and key: %s", credentialsBucketName, hubSpotCredentialsObjectKey));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputStreamToString(s3Object.getObjectContent()), HubSpotCredentials.class);
    }

    private OutreachCredentials getOutreachCredentials(AmazonS3 s3Client, String credentialsBucketName, String outreachCredentialsObjectKey) throws IOException {
        LOGGER.log("Getting Outreach credentials...");
        S3Object s3Object = s3Client.getObject(new GetObjectRequest(credentialsBucketName, outreachCredentialsObjectKey));
        if (s3Object == null) {
            throw new IllegalArgumentException(String.format("Could not get object with bucket: %s, and key: %s", credentialsBucketName, outreachCredentialsObjectKey));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputStreamToString(s3Object.getObjectContent()), OutreachCredentials.class);
    }

    private Map<String, String> getHubSpotOutreachFieldMapping(AmazonS3 s3Client, String lookupBucketName, String hubSpotOutreachFieldMappingObjectKey) throws IOException {
        LOGGER.log("Getting HubSpot-Outreach field mapping...");
        S3Object s3Object = s3Client.getObject(new GetObjectRequest(lookupBucketName, hubSpotOutreachFieldMappingObjectKey));
        if (s3Object == null) {
            throw new IllegalArgumentException(String.format("Could not get object with bucket: %s, and key: %s", lookupBucketName, hubSpotOutreachFieldMappingObjectKey));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputStreamToString(s3Object.getObjectContent()), Map.class);
    }

    private static class S3Lookup implements Lookup {
        private LambdaLogger LOGGER;
        private String lookupBucketName;
        private String lookupKey;
        AmazonS3 s3Client;

        S3Lookup(AmazonS3 s3Client, String lookupBucketName, String lookupKey, LambdaLogger logger) {
            this.s3Client = s3Client;
            this.lookupBucketName = lookupBucketName;
            this.lookupKey = lookupKey;
            this.LOGGER = logger;
        }

        @Override
        public long getLastProcessedTimestamp() throws IOException {
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(lookupBucketName, lookupKey));
            if (s3Object == null) {
                throw new IllegalArgumentException(String.format("Could not get object with bucket: %s, and key: %s", lookupBucketName, lookupKey));
            }
            long lastProcessedTimestamp = Long.parseLong(inputStreamToString(s3Object.getObjectContent()));

            LOGGER.log(String.format("Got last processed timestamp: %d", lastProcessedTimestamp));
            return lastProcessedTimestamp;
        }

        @Override
        public void setLastProcessedTimestamp(long lastProcessedTimestamp) {
            LOGGER.log(String.format("Persisting last processed timestamp: %d", lastProcessedTimestamp));
            s3Client.putObject(lookupBucketName, lookupKey, String.valueOf(lastProcessedTimestamp));
        }
    }

    private static String inputStreamToString(InputStream input) throws IOException {
        StringBuilder objectStringBuilder = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = reader.readLine()) != null) {
            objectStringBuilder.append(line);
        }
        return objectStringBuilder.toString();
    }
}