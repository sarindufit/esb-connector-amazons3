/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.amazons3.operations;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.amazons3.connection.S3ConnectionHandler;
import org.wso2.carbon.connector.amazons3.constants.S3Constants;
import org.wso2.carbon.connector.amazons3.convertors.S3POJOHandler;
import org.wso2.carbon.connector.amazons3.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.amazons3.pojo.LifecycleConfiguration;
import org.wso2.carbon.connector.amazons3.pojo.ObjectVersionConfiguration;
import org.wso2.carbon.connector.amazons3.pojo.S3OperationResult;
import org.wso2.carbon.connector.amazons3.utils.Error;
import org.wso2.carbon.connector.amazons3.utils.S3ConnectorUtils;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketAclRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAclResponse;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentResponse;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.RequestPaymentConfiguration;
import software.amazon.awssdk.services.s3.model.ReplicationRule;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;
import software.amazon.awssdk.services.s3.model.WebsiteConfiguration;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements bucket related operations
 */
public class BucketOperations extends AbstractConnector {
    S3POJOHandler s3POJOHandler = new S3POJOHandler();

    public final void connect(final MessageContext messageContext) throws ConnectException {

        String operationName = (String) messageContext.getProperty(S3Constants.OPERATION_NAME);
        String errorMessage = "";
        String connectorName = S3Constants.CONNECTOR_NAME;
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();

        String bucketName, bucketRegion, delimiter, encodingType, marker, prefix, requestPayer, versionIdMarker, policy,
                token, payer, mfa, status, mfaDelete, keyMarker, uploadIdMarker, acl, grantFullControl, grantRead,
                grantReadACP, grantWrite, grantWriteACP;
        int maxKeys, maxUploads;
        AccessControlPolicy s3AccessControlPolicy = AccessControlPolicy.builder().build();
        WebsiteConfiguration s3WebsiteConfiguration = WebsiteConfiguration.builder().build();
        List<CORSRule> s3CORSRules = new ArrayList<>();
        List<LifecycleRule> s3LifecycleRules = new ArrayList<>();
        List<Tag> s3TagSet = new ArrayList<>();
        ReplicationConfiguration s3ReplicationConfiguration = ReplicationConfiguration.builder().build();

        try {
            String connectionName = S3ConnectorUtils.getConnectionName(messageContext);

            //get s3 client
            S3ConnectionHandler s3ConnectionHandler = (S3ConnectionHandler) handler
                    .getConnection(connectorName, connectionName);
            S3Client s3Client = s3ConnectionHandler.getS3Client();

            //read inputs
            bucketName = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "bucketName");
            bucketRegion = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "bucketRegion");
            acl = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "acl");
            grantFullControl = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "grantFullControl");
            grantRead = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "grantRead");
            grantReadACP = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "grantReadACP");
            grantWrite = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "grantWrite");
            grantWriteACP = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "grantWriteACP");
            Object objectLockEnabledForBucketObj = ConnectorUtils.
                    lookupTemplateParamater(messageContext, "objectLockEnabledForBucket");
            delimiter = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "delimiter");
            encodingType = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "encodingType");
            marker = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "marker");
            Object maxKeysObj = ConnectorUtils.
                    lookupTemplateParamater(messageContext, "maxKeys");
            maxKeys = maxKeysObj != null ? Integer.valueOf((String) maxKeysObj) : 1000;
            prefix = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "prefix");
            requestPayer = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "requestPayer");
            versionIdMarker = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "versionIdMarker");
            String accessControlPolicyStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "accessControlList");
            if (StringUtils.isNotEmpty(accessControlPolicyStr)) {
                org.wso2.carbon.connector.amazons3.pojo.AccessControlPolicy configuration =
                        s3POJOHandler.xmlToObject(accessControlPolicyStr,
                        org.wso2.carbon.connector.amazons3.pojo.AccessControlPolicy.class);
                s3AccessControlPolicy = s3POJOHandler.castAccessControlPolicy(configuration);
            }
            String corsConfigurationStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "corsConfiguration");
            if (StringUtils.isNotEmpty(corsConfigurationStr)) {
                List<org.wso2.carbon.connector.amazons3.pojo.CORSRule> corsRules =
                        s3POJOHandler.xmlToObject(corsConfigurationStr,
                                org.wso2.carbon.connector.amazons3.pojo.CORSConfiguration.class).getCorsRules();
                s3CORSRules = s3POJOHandler.castCORSRules(corsRules);
            }
            String lifecycleConfigurationStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "lifecycleConfiguration");
            if (StringUtils.isNotEmpty(lifecycleConfigurationStr)) {
                List<org.wso2.carbon.connector.amazons3.pojo.LifecycleRule> lifecycleRules =
                        s3POJOHandler.xmlToObject(lifecycleConfigurationStr,
                                LifecycleConfiguration.class).getRules();
                s3LifecycleRules = s3POJOHandler.castLifecycleRules(lifecycleRules);
            }
            policy = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "bucketPolicy");
            Object confirmRemoveSelfBucketAccessObj = ConnectorUtils.
                    lookupTemplateParamater(messageContext, "confirmRemoveSelfBucketAccess");
            token = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "token");
            String replicationConfigurationStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "replicationConfiguration");
            if (StringUtils.isNotEmpty(replicationConfigurationStr)) {
                org.wso2.carbon.connector.amazons3.pojo.ReplicationConfiguration configuration =
                        s3POJOHandler.xmlToObject(replicationConfigurationStr,
                        org.wso2.carbon.connector.amazons3.pojo.ReplicationConfiguration.class);
                s3ReplicationConfiguration = s3POJOHandler.castReplicationConfiguration(configuration);
            }
            payer = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "payer");
            String tagSetStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "tagSet");
            if (StringUtils.isNotEmpty(tagSetStr)) {
                List<org.wso2.carbon.connector.amazons3.pojo.Tag> tags =
                        s3POJOHandler.xmlToObject(tagSetStr,
                                org.wso2.carbon.connector.amazons3.pojo.TagConfiguration.class).getTags();
                s3TagSet = s3POJOHandler.castTags(tags);
            }
            status = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "status");
            mfa = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "mfa");
            mfaDelete = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "mfaDelete");
            String websiteConfigurationStr = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "websiteConfig");
            if (StringUtils.isNotEmpty(websiteConfigurationStr)) {
                org.wso2.carbon.connector.amazons3.pojo.WebsiteConfiguration configuration =
                        s3POJOHandler.xmlToObject(websiteConfigurationStr,
                        org.wso2.carbon.connector.amazons3.pojo.WebsiteConfiguration.class);
                s3WebsiteConfiguration = s3POJOHandler.castWebsiteConfiguration(configuration);
            }
            keyMarker = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "keyMarker");
            uploadIdMarker = (String) ConnectorUtils.
                    lookupTemplateParamater(messageContext, "uploadIdMarker");
            Object maxUploadsObj = ConnectorUtils.
                    lookupTemplateParamater(messageContext, "maxUploads");
            maxUploads = maxUploadsObj != null ? Integer.valueOf((String) maxUploadsObj) : 1000;

            //call the operations
            switch (operationName) {
                case S3Constants.OPERATION_CREATE_BUCKET:
                    errorMessage = "Error while performing bucket creation";
                    createBucket(operationName, s3Client, bucketName, Region.of(bucketRegion), acl, grantFullControl,
                            grantRead, grantReadACP, grantWrite, grantWriteACP, objectLockEnabledForBucketObj,
                            messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET:
                    errorMessage = "Error while deleting the bucket";
                    deleteBucket(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_CORS:
                    errorMessage = "Error while deleting the bucket CORS";
                    deleteBucketCORS(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_LIFECYCLE:
                    errorMessage = "Error while deleting the bucket lifecycle";
                    deleteBucketLifecycle(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_POLICY:
                    errorMessage = "Error while deleting the bucket policy";
                    deleteBucketPolicy(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_REPLICATION:
                    errorMessage = "Error while deleting the bucket replication";
                    deleteBucketReplication(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_TAGGING:
                    errorMessage = "Error while deleting the bucket tagging";
                    deleteBucketTagging(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_DELETE_BUCKET_WEBSITE:
                    errorMessage = "Error while deleting the bucket website configuration";
                    deleteBucketWebsite(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_ACL:
                    errorMessage = "Error while retrieving the bucket ACL";
                    getBucketACL(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_CORS:
                    errorMessage = "Error while retrieving the bucket ACL";
                    getBucketCORS(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_LIFECYCLE_CONFIGURATION:
                    errorMessage = "Error while retrieving the bucket lifecycle configuration";
                    getBucketLifecycleConfiguration(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_LOCATION:
                    errorMessage = "Error while retrieving the bucket location";
                    getBucketLocation(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_LOGGING:
                    errorMessage = "Error while retrieving the bucket logging";
                    getBucketLogging(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_NOTIFICATION_CONFIGURATION:
                    errorMessage = "Error while retrieving the bucket notification configuration";
                    getBucketNotificationConfiguration(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_POLICY:
                    errorMessage = "Error while retrieving the bucket policy";
                    getBucketPolicy(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_REPLICATION:
                    errorMessage = "Error while retrieving the bucket replication";
                    getBucketReplication(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_REQUEST_PAYMENT:
                    errorMessage = "Error while retrieving the bucket request payment";
                    getBucketRequestPayment(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_TAGGING:
                    errorMessage = "Error while retrieving the bucket tagging";
                    getBucketTagging(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_VERSIONING:
                    errorMessage = "Error while retrieving the bucket versioning";
                    getBucketVersioning(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_GET_BUCKET_WEBSITE:
                    errorMessage = "Error while retrieving the bucket website configuration";
                    getBucketWebsite(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_HEAD_BUCKET:
                    errorMessage = "Error while retrieving the user access permission of the bucket";
                    headBucket(operationName, s3Client, bucketName, messageContext);
                    break;
                case S3Constants.OPERATION_LIST_BUCKETS:
                    errorMessage = "Error while retrieving the list of buckets";
                    listBuckets(operationName, s3Client, messageContext);
                    break;
                case S3Constants.OPERATION_LIST_MULTIPART_UPLOADS:
                    errorMessage = "Error while retrieving the bucket website configuration";
                    listMultipartUploads(operationName, s3Client, bucketName, delimiter, encodingType, keyMarker,
                            maxUploads, prefix, uploadIdMarker, messageContext);
                    break;
                case S3Constants.OPERATION_LIST_OBJECTS://tested
                    errorMessage = "Error while listing the objects of the bucket";
                    listObjects(operationName, s3Client, bucketName, delimiter, encodingType,
                            marker, maxKeys, prefix, requestPayer, messageContext);
                    break;
                case S3Constants.OPERATION_LIST_OBJECT_VERSIONS:
                    errorMessage = "Error while retrieving the object Versioning";
                    listObjectVersions(operationName, s3Client, bucketName, delimiter, encodingType, keyMarker, maxKeys,
                            prefix, versionIdMarker, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_ACL:
                    errorMessage = "Error while creating the bucket ACL";
                    putBucketACL(operationName, s3Client, acl, s3AccessControlPolicy, bucketName, grantFullControl,
                            grantRead, grantReadACP, grantWrite, grantWriteACP, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_CORS:
                    errorMessage = "Error while creating the bucket CORS";
                    putBucketCORS(operationName, s3Client, bucketName, s3CORSRules, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_LIFECYCLE_CONFIGURATION:
                    errorMessage = "Error while creating the bucket lifecycle configuration";
                    putBucketLifecycleConfiguration(operationName, s3Client, bucketName, s3LifecycleRules,
                            messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_POLICY:
                    errorMessage = "Error while creating the bucket policy";
                    putBucketPolicy(operationName, s3Client, bucketName, policy, confirmRemoveSelfBucketAccessObj,
                            messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_REPLICATION:
                    errorMessage = "Error while creating the bucket replication";
                    putBucketReplication(operationName, s3Client, bucketName, s3ReplicationConfiguration, token,
                            messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_REQUEST_PAYMENT:
                    errorMessage = "Error while creating the bucket request payment";
                    putBucketRequestPayment(operationName, s3Client, bucketName, payer, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_TAGGING:
                    errorMessage = "Error while creating the bucket tagging";
                    putBucketTagging(operationName, s3Client, bucketName, s3TagSet, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_VERSIONING:
                    errorMessage = "Error while creating the bucket versioning";
                    putBucketVersioning(operationName, s3Client, bucketName, mfa, status, mfaDelete, messageContext);
                    break;
                case S3Constants.OPERATION_PUT_BUCKET_WEBSITE:
                    errorMessage = "Error while creating the bucket website";
                    putBucketWebsite(operationName, s3Client, bucketName, s3WebsiteConfiguration, messageContext);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operation: " + operationName);
            }
        } catch (InvalidConfigurationException e) {
            S3OperationResult result = new S3OperationResult(
                    operationName,
                    false,
                    Error.INVALID_CONFIGURATION,
                    errorMessage);

            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException(errorMessage, e, messageContext);
        }
    }

    public void createBucket(String operationName, S3Client s3Client, String bucketName, Region region, String acl,
                             String grantFullControl, String grantRead, String grantReadACP, String grantWrite,
                             String grantWriteACP, Object objectLockEnabledForBucket, MessageContext messageContext) {
        S3OperationResult result;
        CreateBucketRequest request = CreateBucketRequest
                .builder()
                .acl(acl)
                .bucket(bucketName)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .locationConstraint(region.id())
                                .build())
                .grantFullControl(grantFullControl)
                .grantRead(grantRead)
                .grantReadACP(grantReadACP)
                .grantWrite(grantWrite)
                .grantWriteACP(grantWriteACP)
                .objectLockEnabledForBucket(objectLockEnabledForBucket != null ?
                        Boolean.valueOf((String) objectLockEnabledForBucket) : null)
                .build();
        try {
            CreateBucketResponse createRes = s3Client.createBucket(request);
            SdkHttpResponse sdkHttpResponse = createRes.sdkHttpResponse();
            OMElement responseElement = S3ConnectorUtils.createOMElement("Response", "");
            responseElement.addChild(S3ConnectorUtils.createOMElement("Status",
                    Integer.toString(sdkHttpResponse.statusCode())
                    + ":" + sdkHttpResponse.statusText()));
            responseElement.addChild(S3ConnectorUtils.createOMElement("Location", createRes.location()));
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            Error error = Error.CONFLICT;
            result = new S3OperationResult(
                    operationName,
                    false,
                    error,
                    "Bucket already exists");
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (IllegalArgumentException e) {
            handleException("Error occurred while creating the bucket: " + e.getMessage(), e, messageContext);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service: " + e.getMessage(), e, messageContext);
        }
    }

    public void deleteBucket(String operationName, S3Client s3Client, String bucketName,
                             MessageContext messageContext) {
        S3OperationResult result;
        ObjectOperations objectOperations = new ObjectOperations();
        try {
            boolean isTruncated = true;
            while (isTruncated) {
                ListObjectsV2Iterable objectListing = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .build());
                for (ListObjectsV2Response response : objectListing) {
                    /*
                     * There could be many pages in object list response is just one page
                     *
                     */
                    for (S3Object s3Object : response.contents()) {
                        objectOperations.deleteObject(operationName + "/deleteObject", s3Client,
                                bucketName, s3Object.key(), null, null, null, null, messageContext);
                    }
                    isTruncated = response.isTruncated();
                }
            }

            // Delete all object versions (required for versioned buckets).
            ListObjectVersionsIterable versionList =
                    s3Client.listObjectVersionsPaginator(ListObjectVersionsRequest.builder()
                    .bucket(bucketName)
                    .build());
            isTruncated = true;
            while (isTruncated) {
                for (ListObjectVersionsResponse response : versionList) {
                    for (ObjectVersion version : response.versions()) {
                        objectOperations.deleteObject(operationName + "/deleteObject", s3Client,
                                bucketName, version.key(), null, version.versionId(), null, null, messageContext);
                    }
                    isTruncated = response.isTruncated();
                }
            }

            deleteEmptyBucket(operationName, s3Client, bucketName, messageContext);
        } catch (NoSuchBucketException e) {
            Error error = Error.NOT_FOUND;
            result = new S3OperationResult(
                    operationName,
                    false,
                    error,
                    "Bucket is not found");
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void deleteEmptyBucket(String operationName, S3Client s3Client, String bucketName,
                                  MessageContext messageContext) throws AwsServiceException, SdkClientException {
        S3OperationResult result;
        DeleteBucketRequest request = DeleteBucketRequest.builder().bucket(bucketName).build();
        DeleteBucketResponse delRes = s3Client.deleteBucket(request);
        SdkHttpResponse sdkHttpResponse = delRes.sdkHttpResponse();
        result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
        S3ConnectorUtils.setResultAsPayload(messageContext, result);
    }

    public void deleteBucketCORS(String operationName, S3Client s3Client, String bucketName,
                                 MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketCorsRequest request = DeleteBucketCorsRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketCorsResponse response = s3Client.deleteBucketCors(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void deleteBucketLifecycle(String operationName, S3Client s3Client, String bucketName,
                                      MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketLifecycleRequest request = DeleteBucketLifecycleRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketLifecycleResponse response = s3Client.deleteBucketLifecycle(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        }
    }

    public void deleteBucketPolicy(String operationName, S3Client s3Client, String bucketName,
                                   MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketPolicyRequest request = DeleteBucketPolicyRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketPolicyResponse response = s3Client.deleteBucketPolicy(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void deleteBucketReplication(String operationName, S3Client s3Client, String bucketName,
                                        MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketReplicationRequest request = DeleteBucketReplicationRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketReplicationResponse response = s3Client.deleteBucketReplication(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void deleteBucketTagging(String operationName, S3Client s3Client, String bucketName,
                                    MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketTaggingRequest request = DeleteBucketTaggingRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketTaggingResponse response = s3Client.deleteBucketTagging(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void deleteBucketWebsite(String operationName, S3Client s3Client, String bucketName,
                                    MessageContext messageContext) {
        S3OperationResult result;
        DeleteBucketWebsiteRequest request = DeleteBucketWebsiteRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            DeleteBucketWebsiteResponse delRes = s3Client.deleteBucketWebsite(request);
            SdkHttpResponse sdkHttpResponse = delRes.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketACL(String operationName, S3Client s3Client, String bucketName,
                             MessageContext messageContext) {
        S3OperationResult result;
        GetBucketAclRequest request = GetBucketAclRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketAclResponse response = s3Client.getBucketAcl(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("AccessControlPolicy", "");
            List<Grant> grants = response.grants();
            org.wso2.carbon.connector.amazons3.pojo.Owner owner = s3POJOHandler.castS3Owner(response.owner());
            String ownerString =
                    s3POJOHandler.getObjectAsXml(owner, org.wso2.carbon.connector.amazons3.pojo.Owner.class);
            try {
                responseElement.addChild(AXIOMUtil.stringToOM(ownerString));
                for (Grant s3Grant : grants) {
                    org.wso2.carbon.connector.amazons3.pojo.Grant grant = s3POJOHandler.castS3Grant(s3Grant);
                    String xmlString =
                            s3POJOHandler.getObjectAsXml(grant, org.wso2.carbon.connector.amazons3.pojo.Grant.class);
                    responseElement.addChild(AXIOMUtil.stringToOM(xmlString));
                }
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned lifecycle configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketCORS(String operationName, S3Client s3Client, String bucketName,
                              MessageContext messageContext) {
        S3OperationResult result;
        GetBucketCorsRequest request = GetBucketCorsRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketCorsResponse response = s3Client.getBucketCors(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("CORSConfiguration", "");
            List<CORSRule> corsRules = response.corsRules();
            for (CORSRule corsRule : corsRules) {
                org.wso2.carbon.connector.amazons3.pojo.CORSRule rule = s3POJOHandler.castS3CORSRule(corsRule);
                String xmlString =
                        s3POJOHandler.getObjectAsXml(rule, org.wso2.carbon.connector.amazons3.pojo.CORSRule.class);
                try {
                    responseElement.addChild(AXIOMUtil.stringToOM(xmlString));
                } catch (XMLStreamException e) {
                    handleException("Unable to process the returned CORS configuration: " + e.getMessage(), e,
                            messageContext);
                }
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketLifecycleConfiguration(String operationName, S3Client s3Client, String bucketName,
                                                MessageContext messageContext) {
        S3OperationResult result;
        GetBucketLifecycleConfigurationRequest request = GetBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketLifecycleConfigurationResponse response = s3Client.getBucketLifecycleConfiguration(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("LifecycleConfiguration", "");
            List<LifecycleRule> lifecycleRules = response.rules();
            for (LifecycleRule lifecycleRule : lifecycleRules) {
                org.wso2.carbon.connector.amazons3.pojo.LifecycleRule rule =
                        s3POJOHandler.castS3LifecycleRule(lifecycleRule);
                String xmlString =
                        s3POJOHandler.getObjectAsXml(rule, org.wso2.carbon.connector.amazons3.pojo.LifecycleRule.class);
                try {
                    responseElement.addChild(AXIOMUtil.stringToOM(xmlString));
                } catch (XMLStreamException e) {
                    handleException("Unable to process the returned lifecycle configuration: " + e.getMessage(), e,
                            messageContext);
                }
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            handleException(e.awsErrorDetails().errorMessage(), e, messageContext);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketLocation(String operationName, S3Client s3Client, String bucketName,
                                  MessageContext messageContext) {
        S3OperationResult result;
        GetBucketLocationRequest request = GetBucketLocationRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketLocationResponse response = s3Client.getBucketLocation(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("LocationConstraint",
                    response.locationConstraintAsString());
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketLogging(String operationName, S3Client s3Client, String bucketName,
                                 MessageContext messageContext) {
        S3OperationResult result;
        GetBucketLoggingRequest request = GetBucketLoggingRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketLoggingResponse response = s3Client.getBucketLogging(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("BucketLoggingStatus", "");
            org.wso2.carbon.connector.amazons3.pojo.LoggingEnabled loggingEnabled =
                    s3POJOHandler.castS3LoggingEnabled(response.loggingEnabled());
            String xmlString = s3POJOHandler.getObjectAsXml(loggingEnabled,
                    org.wso2.carbon.connector.amazons3.pojo.LoggingEnabled.class);
            try {
                responseElement.addChild(AXIOMUtil.stringToOM(xmlString));
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned logging configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketNotificationConfiguration(String operationName, S3Client s3Client, String bucketName,
                                                   MessageContext messageContext) {
        S3OperationResult result;
        GetBucketNotificationConfigurationRequest request = GetBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketNotificationConfigurationResponse response = s3Client.getBucketNotificationConfiguration(request);
            OMElement responseElement =
                    S3ConnectorUtils.createOMElement("NotificationConfiguration", "");
            org.wso2.carbon.connector.amazons3.pojo.NotificationConfiguration config =
                    s3POJOHandler.castS3NotificationConfiguration(response);
            String xmlString = s3POJOHandler.getObjectAsXml(config,
                    org.wso2.carbon.connector.amazons3.pojo.NotificationConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned notification configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketPolicy(String operationName, S3Client s3Client, String bucketName,
                                MessageContext messageContext) {
        S3OperationResult result;
        GetBucketPolicyRequest request = GetBucketPolicyRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketPolicyResponse response = s3Client.getBucketPolicy(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("PolicyConfiguration", "");
            OMElement childElement = S3ConnectorUtils.createOMElement("Policy", response.policy());
            responseElement.addChild(childElement);
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketReplication(String operationName, S3Client s3Client, String bucketName,
                                     MessageContext messageContext) {
        S3OperationResult result;
        GetBucketReplicationRequest request = GetBucketReplicationRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketReplicationResponse response = s3Client.getBucketReplication(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("ReplicationConfiguration", "");
            org.wso2.carbon.connector.amazons3.pojo.ReplicationConfiguration replicationConfiguration =
                    s3POJOHandler.castS3ReplicationConfiguration(response.replicationConfiguration());
            String xmlString = s3POJOHandler.getObjectAsXml(replicationConfiguration,
                    org.wso2.carbon.connector.amazons3.pojo.ReplicationConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned replication configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketRequestPayment(String operationName, S3Client s3Client, String bucketName,
                                        MessageContext messageContext) {
        S3OperationResult result;
        GetBucketRequestPaymentRequest request = GetBucketRequestPaymentRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketRequestPaymentResponse response = s3Client.getBucketRequestPayment(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("RequestPaymentConfiguration", "");
            OMElement childElement = S3ConnectorUtils.createOMElement("Payer", response.payerAsString());
            responseElement.addChild(childElement);
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketTagging(String operationName, S3Client s3Client, String bucketName,
                                 MessageContext messageContext) {
        S3OperationResult result;
        GetBucketTaggingRequest request = GetBucketTaggingRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketTaggingResponse response = s3Client.getBucketTagging(request);
            OMElement childElement = S3ConnectorUtils.createOMElement("TagSet", "");
            for (Tag s3Tag : response.tagSet()) {
                org.wso2.carbon.connector.amazons3.pojo.Tag tag = s3POJOHandler.castS3Tag(s3Tag);
                String xmlString = s3POJOHandler.getObjectAsXml(tag, org.wso2.carbon.connector.amazons3.pojo.Tag.class);
                try {
                    childElement.addChild(AXIOMUtil.stringToOM(xmlString));
                } catch (XMLStreamException e) {
                    handleException("Unable to process the returned tag configuration: " + e.getMessage(), e,
                            messageContext);
                }
            }
            OMElement responseElement = S3ConnectorUtils.createOMElement("Tagging", childElement);
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketVersioning(String operationName, S3Client s3Client, String bucketName,
                                    MessageContext messageContext) {
        S3OperationResult result;
        GetBucketVersioningRequest request = GetBucketVersioningRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketVersioningResponse response = s3Client.getBucketVersioning(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("VersioningConfiguration", "");
            org.wso2.carbon.connector.amazons3.pojo.BucketVersioningConfiguration configuration =
                    s3POJOHandler.castS3BucketVersioningConfiguration(response);
            String xmlString = s3POJOHandler.getObjectAsXml(configuration,
                    org.wso2.carbon.connector.amazons3.pojo.BucketVersioningConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned bucket versioning configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void getBucketWebsite(String operationName, S3Client s3Client, String bucketName,
                                 MessageContext messageContext) {
        S3OperationResult result;
        GetBucketWebsiteRequest request = GetBucketWebsiteRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            GetBucketWebsiteResponse response = s3Client.getBucketWebsite(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("WebsiteConfiguration", "");
            org.wso2.carbon.connector.amazons3.pojo.WebsiteConfiguration configuration =
                    s3POJOHandler.castS3WebsiteConfiguration(response);
            String xmlString = s3POJOHandler.getObjectAsXml(configuration,
                    org.wso2.carbon.connector.amazons3.pojo.WebsiteConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned website configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void headBucket(String operationName, S3Client s3Client, String bucketName, MessageContext messageContext) {
        S3OperationResult result;
        HeadBucketRequest request = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        try {
            HeadBucketResponse response = s3Client.headBucket(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (NoSuchBucketException e) {
            Error error = Error.NOT_FOUND;
            result = new S3OperationResult(
                    operationName,
                    false,
                    error,
                    "Bucket is not found");
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void listBuckets(String operationName, S3Client s3Client, MessageContext messageContext) {
        S3OperationResult result;
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        try {
            ListBucketsResponse response = s3Client.listBuckets(listBucketsRequest);
            OMElement responseElement = S3ConnectorUtils.createOMElement("ListAllMyBucketsResult", "");
            org.wso2.carbon.connector.amazons3.pojo.BucketsConfiguration bucketsConfiguration =
                    s3POJOHandler.castS3BucketsConfiguration(response);
            String xmlString = s3POJOHandler.getObjectAsXml(bucketsConfiguration,
                    org.wso2.carbon.connector.amazons3.pojo.BucketsConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned buckets configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void listMultipartUploads(String operationName, S3Client s3Client, String bucketName, String delimiter,
                                     String encodingType, String keyMarker, int maxUploads, String prefix,
                                     String uploadIdMarker, MessageContext messageContext) {
        S3OperationResult result;
        ListMultipartUploadsRequest request = ListMultipartUploadsRequest.builder()
                .bucket(bucketName)
                .delimiter(delimiter)
                .encodingType(encodingType)
                .keyMarker(keyMarker)
                .maxUploads(maxUploads)
                .prefix(prefix)
                .uploadIdMarker(uploadIdMarker)
                .build();
        try {
            ListMultipartUploadsResponse response = s3Client.listMultipartUploads(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("ListMultipartUploadsResult", "");
            org.wso2.carbon.connector.amazons3.pojo.MultipartUploads multipartUpload =
                    s3POJOHandler.castS3MultipartUploads(response);
            String xmlString = s3POJOHandler.getObjectAsXml(multipartUpload,
                    org.wso2.carbon.connector.amazons3.pojo.MultipartUploads.class);
            try {
                responseElement.addChild(AXIOMUtil.stringToOM(xmlString));
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned multipart uploads: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void listObjects(String operationName, S3Client s3Client, String bucketName, String delimiter,
                            String encodingType, String marker, int maxKeys, String prefix, String requestPayer,
                            MessageContext messageContext) throws NoSuchBucketException {
        S3OperationResult result;
        ListObjectsRequest request = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .delimiter(delimiter)
                .encodingType(encodingType)
                .marker(marker)
                .maxKeys(maxKeys)
                .prefix(prefix)
                .requestPayer(requestPayer)
                .build();
        try {
            ListObjectsResponse response = s3Client.listObjects(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("ListBucketResult", "");
            org.wso2.carbon.connector.amazons3.pojo.ObjectConfiguration configuration =
                    s3POJOHandler.castS3Objects(response);
            String xmlString = s3POJOHandler.getObjectAsXml(configuration,
                    org.wso2.carbon.connector.amazons3.pojo.ObjectConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned bucket objects: " + e.getMessage(), e, messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(),
                    operationName, Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void listObjectVersions(String operationName, S3Client s3Client, String bucketName, String delimiter,
                                   String encodingType, String keyMarker, int maxKeys, String prefix,
                                   String versionIdMarker, MessageContext messageContext) {
        S3OperationResult result;
        ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .delimiter(delimiter)
                .encodingType(encodingType)
                .keyMarker(keyMarker)
                .maxKeys(maxKeys)
                .prefix(prefix)
                .versionIdMarker(versionIdMarker)
                .build();
        try {
            ListObjectVersionsResponse response = s3Client.listObjectVersions(request);
            OMElement responseElement = S3ConnectorUtils.createOMElement("ListVersionsResult", "");
            ObjectVersionConfiguration config = s3POJOHandler.castS3ObjectVersions(response);
            String xmlString = s3POJOHandler.getObjectAsXml(config, ObjectVersionConfiguration.class);
            try {
                responseElement = AXIOMUtil.stringToOM(xmlString);
            } catch (XMLStreamException e) {
                handleException("Unable to process the returned object versions configuration: " + e.getMessage(), e,
                        messageContext);
            }
            result = new S3OperationResult(
                    operationName,
                    true, responseElement);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketACL(String operationName, S3Client s3Client, String acl,
                             AccessControlPolicy accessControlPolicy, String bucketName, String grantFullControl,
                             String grantRead, String grantReadACP, String grantWrite, String grantWriteACP,
                             MessageContext messageContext) {
        S3OperationResult result;
        PutBucketAclRequest request = PutBucketAclRequest.builder()
                .acl(acl)
                .accessControlPolicy(accessControlPolicy)
                .bucket(bucketName)
                .grantFullControl(grantFullControl)
                .grantRead(grantRead)
                .grantReadACP(grantReadACP)
                .grantWrite(grantWrite)
                .grantWriteACP(grantWriteACP)
                .build();
        try {
            PutBucketAclResponse response = s3Client.putBucketAcl(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketCORS(String operationName, S3Client s3Client, String bucketName, List<CORSRule> corsRules,
                              MessageContext messageContext) {

        S3OperationResult result;
        PutBucketCorsRequest request = PutBucketCorsRequest
                .builder()
                .bucket(bucketName)
                .corsConfiguration(CORSConfiguration.builder().corsRules(corsRules).build())
                .build();
        try {
            PutBucketCorsResponse response = s3Client.putBucketCors(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketLifecycleConfiguration(String operationName, S3Client s3Client, String bucketName,
                                                List<LifecycleRule> lifecycleRules, MessageContext messageContext) {
        S3OperationResult result;
        PutBucketLifecycleConfigurationRequest request = PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(lifecycleRules).build())
                .build();
        try {
            PutBucketLifecycleConfigurationResponse response = s3Client.putBucketLifecycleConfiguration(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketPolicy(String operationName, S3Client s3Client, String bucketName, String policy,
                                Object confirmRemoveSelfBucketAccessObj, MessageContext messageContext) {
        S3OperationResult result;
        PutBucketPolicyRequest request = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .confirmRemoveSelfBucketAccess(confirmRemoveSelfBucketAccessObj != null ?
                        Boolean.valueOf((String) confirmRemoveSelfBucketAccessObj) : null)
                .build();
        try {
            PutBucketPolicyResponse response = s3Client.putBucketPolicy(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketReplication(String operationName, S3Client s3Client, String bucketName,
                                     ReplicationConfiguration replicationConfiguration, String token,
                                     MessageContext messageContext) {
        S3OperationResult result;
        PutBucketReplicationRequest request = PutBucketReplicationRequest.builder()
                .bucket(bucketName)
                .replicationConfiguration(replicationConfiguration)
                .token(token)
                .build();
        try {
            PutBucketReplicationResponse response = s3Client.putBucketReplication(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketRequestPayment(String operationName, S3Client s3Client, String bucketName, String payer,
                                        MessageContext messageContext) {
        S3OperationResult result;
        PutBucketRequestPaymentRequest request = PutBucketRequestPaymentRequest.builder()
                .bucket(bucketName)
                .requestPaymentConfiguration(RequestPaymentConfiguration.builder()
                        .payer(payer)
                        .build())
                .build();
        try {
            PutBucketRequestPaymentResponse response = s3Client.putBucketRequestPayment(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketTagging(String operationName, S3Client s3Client, String bucketName, List<Tag> tagSet,
                                 MessageContext messageContext) {
        S3OperationResult result;
        PutBucketTaggingRequest request = PutBucketTaggingRequest.builder()
                .bucket(bucketName)
                .tagging(Tagging.builder().tagSet(tagSet).build())
                .build();
        try {
            PutBucketTaggingResponse response = s3Client.putBucketTagging(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        }
    }

    public void putBucketVersioning(String operationName, S3Client s3Client, String bucketName, String mfa,
                                    String status, String mfaDelete, MessageContext messageContext) {
        S3OperationResult result;
        PutBucketVersioningRequest request = PutBucketVersioningRequest.builder()
                .bucket(bucketName)
                .mfa(mfa)
                .versioningConfiguration(VersioningConfiguration.builder()
                        .status(status)
                        .mfaDelete(mfaDelete).build())
                .build();
        try {
            PutBucketVersioningResponse response = s3Client.putBucketVersioning(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }

    public void putBucketWebsite(String operationName, S3Client s3Client, String bucketName,
                                 WebsiteConfiguration websiteConfiguration, MessageContext messageContext) {
        S3OperationResult result;
        PutBucketWebsiteRequest request = PutBucketWebsiteRequest.builder()
                .bucket(bucketName)
                .websiteConfiguration(websiteConfiguration)
                .build();
        try {
            PutBucketWebsiteResponse response = s3Client.putBucketWebsite(request);
            SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
            result = S3ConnectorUtils.getSuccessResult(sdkHttpResponse, operationName);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (S3Exception e) {
            result = S3ConnectorUtils.getFailureResult(e.awsErrorDetails().errorMessage(), operationName,
                    Error.BAD_REQUEST);
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
        } catch (AwsServiceException | SdkClientException e) {
            result = new S3OperationResult(
                    operationName,
                    false,
                    Error.CONNECTION_ERROR,
                    "Error occurred while accessing the AWS SDK service: " + e.getMessage());
            S3ConnectorUtils.setResultAsPayload(messageContext, result);
            handleException("Error occurred while accessing the AWS SDK service", e, messageContext);
        }
    }
}
