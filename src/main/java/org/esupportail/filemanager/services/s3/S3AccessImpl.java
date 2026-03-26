/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.filemanager.services.s3;
import jakarta.annotation.Resource;
import org.esupportail.filemanager.beans.DownloadFile;
import org.esupportail.filemanager.beans.JsTreeFile;
import org.esupportail.filemanager.beans.PresignedUrl;
import org.esupportail.filemanager.beans.UploadActionType;
import org.esupportail.filemanager.beans.UserPassword;
import org.esupportail.filemanager.exceptions.EsupStockException;
import org.esupportail.filemanager.exceptions.EsupStockFileExistException;
import org.esupportail.filemanager.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.filemanager.services.FsAccess;
import org.esupportail.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * Implementation of FsAccess for Amazon S3 compatible storage (AWS S3, MinIO, etc.)
 */
public class S3AccessImpl extends FsAccess implements DisposableBean {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(S3AccessImpl.class);
    @Resource
    ResourceUtils resourceUtils;

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private String bucketName;
    private String region = "us-east-1";
    private boolean pathStyleAccessEnabled = false;
    private String basePath = "";
    private boolean presignedUrlsEnabled = false;
    private int presignedUrlExpirationMinutes = 15;

    public void setResourceUtils(ResourceUtils resourceUtils) {
        this.resourceUtils = resourceUtils;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
        this.pathStyleAccessEnabled = pathStyleAccessEnabled;
    }

    public void setPresignedUrlsEnabled(boolean presignedUrlsEnabled) {
        this.presignedUrlsEnabled = presignedUrlsEnabled;
    }

    public void setPresignedUrlExpirationMinutes(int presignedUrlExpirationMinutes) {
        this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
        if (this.basePath != null && !this.basePath.isEmpty()) {
            if (!this.basePath.endsWith("/")) {
                this.basePath += "/";
            }
            if (this.basePath.startsWith("/")) {
                this.basePath = this.basePath.substring(1);
            }
        }
    }
    @Override
    protected void open() {
        super.open();
        try {
            if (!isOpened()) {
                S3ClientBuilder builder = S3Client.builder();
                // Configure credentials
                if (userAuthenticatorService != null) {
                    UserPassword userPassword = userAuthenticatorService.getUserPassword();
                    AwsBasicCredentials credentials = AwsBasicCredentials.create(
                            userPassword.getUsername(),
                            userPassword.getPassword()
                    );
                    builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
                }
                // Configure region
                if (region != null && !region.isEmpty()) {
                    builder.region(Region.of(region));
                }
                // Configure custom endpoint (for MinIO, Ceph, etc.)
                if (uri != null && !uri.isEmpty()) {
                    builder.endpointOverride(URI.create(uri));
                }
                // Enable path-style access for non-AWS S3 compatible services
                builder.forcePathStyle(pathStyleAccessEnabled);
                s3Client = builder.build();
                // Test connection by checking if bucket exists
                try {
                    s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                } catch (S3Exception e) {
                    log.error("Cannot access bucket {}: {}", bucketName, e.awsErrorDetails().errorMessage());
                    throw new EsupStockException("Cannot access S3 bucket: " + e.awsErrorDetails().errorMessage(), e);
                }
            }
        } catch (S3Exception e) {
            log.error("Error opening S3 connection", e);
            throw new EsupStockException("S3 connection error", e);
        }
    }
    @Override
    public void close() {
        if (s3Presigner != null) {
            s3Presigner.close();
            s3Presigner = null;
        }
        if (s3Client != null) {
            s3Client.close();
            s3Client = null;
        }
    }
    @Override
    public void destroy() throws Exception {
        this.close();
    }
    @Override
    protected boolean isOpened() {
        return s3Client != null;
    }
    /**
     * Get the full S3 key from a relative path
     */
    private String getS3Key(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return basePath.isEmpty() ? "" : basePath;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return basePath + path;
    }
    /**
     * Get the relative path from an S3 key
     */
    private String getRelativePath(String s3Key) {
        if (s3Key.startsWith(basePath)) {
            String path = s3Key.substring(basePath.length());
            return path.isEmpty() ? "/" : path;
        }
        return s3Key;
    }
    /**
     * Check if an S3 key represents a "folder" (ends with /)
     */
    private boolean isFolder(String key) {
        return key.endsWith("/");
    }
    @Override
    public JsTreeFile get(String path, boolean folderDetails, boolean fileDetails) {
        try {
            open();
            String s3Key = getS3Key(path);
            // Root or folder
            if (path.isEmpty() || path.equals("/") || s3Key.isEmpty()) {
                return createJsTreeFileForRoot(folderDetails);
            }
            // Check if it's a folder by trying to list with prefix
            String folderKey = s3Key.endsWith("/") ? s3Key : s3Key + "/";
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderKey)
                    .maxKeys(1)
                    .build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            if (listResponse.hasContents() || listResponse.hasCommonPrefixes()) {
                // It's a folder
                return createJsTreeFileForFolder(path, folderDetails);
            }
            // Try to get as a file
            try {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();
                HeadObjectResponse headResponse = s3Client.headObject(headRequest);
                return createJsTreeFileForObject(path, headResponse, fileDetails);
            } catch (NoSuchKeyException e) {
                log.warn("Object not found: {}", s3Key);
                throw new EsupStockException("Object not found: " + path);
            }
        } catch (S3Exception e) {
            log.error("Error getting S3 object", e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            throw new EsupStockException("S3 error", e);
        }
    }
    @Override
    public List<JsTreeFile> getChildren(String path) {
        try {
            open();
            List<JsTreeFile> files = new ArrayList<>();
            String s3Prefix = getS3Key(path);
            if (!s3Prefix.isEmpty() && !s3Prefix.endsWith("/")) {
                s3Prefix += "/";
            }
            // List objects with delimiter to get immediate children only
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Prefix)
                    .delimiter("/")
                    .build();
            ListObjectsV2Response listResponse;
            String continuationToken = null;
            do {
                if (continuationToken != null) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(continuationToken)
                            .build();
                }
                listResponse = s3Client.listObjectsV2(listRequest);
                // Add folders (common prefixes)
                if (listResponse.hasCommonPrefixes()) {
                    for (CommonPrefix prefix : listResponse.commonPrefixes()) {
                        String folderPath = getRelativePath(prefix.prefix());
                        if (folderPath.endsWith("/")) {
                            folderPath = folderPath.substring(0, folderPath.length() - 1);
                        }
                        files.add(createJsTreeFileForFolder(folderPath, false));
                    }
                }
                // Add files
                if (listResponse.hasContents()) {
                    for (S3Object s3Object : listResponse.contents()) {
                        String key = s3Object.key();
                        // Skip the folder itself and empty folder markers
                        if (!key.equals(s3Prefix) && !key.endsWith("/")) {
                            String filePath = getRelativePath(key);
                            files.add(createJsTreeFileForS3Object(filePath, s3Object, true));
                        }
                    }
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (listResponse.isTruncated());
            return files;
        } catch (NoSuchBucketException e) {
            log.error("Bucket does not exist: {}. Please verify that the bucket '{}' exists in MinIO/S3 at endpoint: {}",
                      bucketName, bucketName, uri);
            throw new EsupStockException("Bucket '" + bucketName + "' does not exist. Please create it in MinIO.", e);
        } catch (S3Exception e) {
            log.error("Error listing S3 objects in bucket '{}' : {}", bucketName, e.getMessage(), e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            throw new EsupStockException("S3 error", e);
        }
    }
    @Override
    public boolean remove(String path) {
        try {
            open();
            String s3Key = getS3Key(path);
            // Check if it's a folder
            if (isFolder(s3Key) || isFolderByListing(s3Key)) {
                return removeFolder(s3Key);
            } else {
                return removeFile(s3Key);
            }
        } catch (S3Exception e) {
            log.error("Error removing S3 object: {}", path, e);
            return false;
        }
    }
    private boolean removeFile(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("File deleted: {}", s3Key);
            return true;
        } catch (S3Exception e) {
            log.error("Error deleting file: {}", s3Key, e);
            return false;
        }
    }
    private boolean removeFolder(String s3Key) {
        try {
            if (!s3Key.endsWith("/")) {
                s3Key += "/";
            }
            // List all objects in the folder
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Key)
                    .build();
            ListObjectsV2Response listResponse;
            String continuationToken = null;
            do {
                if (continuationToken != null) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(continuationToken)
                            .build();
                }
                listResponse = s3Client.listObjectsV2(listRequest);
                if (listResponse.hasContents()) {
                    for (S3Object s3Object : listResponse.contents()) {
                        removeFile(s3Object.key());
                    }
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (listResponse.isTruncated());
            log.info("Folder deleted: {}", s3Key);
            return true;
        } catch (S3Exception e) {
            log.error("Error deleting folder: {}", s3Key, e);
            return false;
        }
    }
    private boolean isFolderByListing(String s3Key) {
        try {
            String folderKey = s3Key.endsWith("/") ? s3Key : s3Key + "/";
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderKey)
                    .maxKeys(1)
                    .build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            return listResponse.hasContents() || listResponse.hasCommonPrefixes();
        } catch (S3Exception e) {
            return false;
        }
    }
    @Override
    public String createFile(String parentPath, String title, String type) {
        try {
            open();
            String parentKey = getS3Key(parentPath);
            if (!parentKey.isEmpty() && !parentKey.endsWith("/")) {
                parentKey += "/";
            }
            String newKey = parentKey + title;
            if ("folder".equals(type)) {
                // Create folder marker
                if (!newKey.endsWith("/")) {
                    newKey += "/";
                }
                // Check if folder already exists
                if (objectExists(newKey)) {
                    log.info("Folder already exists: {}", newKey);
                    return null;
                }
                // Create empty object with trailing slash
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(newKey)
                        .contentLength(0L)
                        .build();
                s3Client.putObject(putRequest, RequestBody.empty());
                log.info("Folder created: {}", newKey);
                return getRelativePath(newKey);
            } else {
                // Create empty file
                if (objectExists(newKey)) {
                    log.info("File already exists: {}", newKey);
                    return null;
                }
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(newKey)
                        .contentLength(0L)
                        .build();
                s3Client.putObject(putRequest, RequestBody.empty());
                log.info("File created: {}", newKey);
                return getRelativePath(newKey);
            }
        } catch (S3Exception e) {
            log.error("Error creating file in S3", e);
            throw new EsupStockException("S3 error", e);
        }
    }
    @Override
    public boolean renameFile(String path, String title) {
        try {
            open();
            String oldKey = getS3Key(path);
            String parentKey = "";
            int lastSlash = oldKey.lastIndexOf('/');
            if (lastSlash > 0) {
                parentKey = oldKey.substring(0, lastSlash + 1);
            }
            String newKey = parentKey + title;
            // Check if target already exists
            if (objectExists(newKey)) {
                log.info("Target file already exists: {}", newKey);
                return false;
            }
            boolean isFolder = isFolder(oldKey) || isFolderByListing(oldKey);
            if (isFolder) {
                return renameFolder(oldKey, newKey);
            } else {
                return renameObject(oldKey, newKey);
            }
        } catch (S3Exception e) {
            log.error("Error renaming file in S3", e);
            return false;
        }
    }
    private boolean renameObject(String oldKey, String newKey) {
        try {
            // Copy object to new key
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(oldKey)
                    .destinationBucket(bucketName)
                    .destinationKey(newKey)
                    .build();
            s3Client.copyObject(copyRequest);
            // Delete old object
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(oldKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Object renamed from {} to {}", oldKey, newKey);
            return true;
        } catch (S3Exception e) {
            log.error("Error renaming object: {} -> {}", oldKey, newKey, e);
            return false;
        }
    }
    private boolean renameFolder(String oldKey, String newKey) {
        try {
            if (!oldKey.endsWith("/")) {
                oldKey += "/";
            }
            if (!newKey.endsWith("/")) {
                newKey += "/";
            }
            // List all objects in the folder
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(oldKey)
                    .build();
            ListObjectsV2Response listResponse;
            String continuationToken = null;
            List<String> keysToDelete = new ArrayList<>();
            do {
                if (continuationToken != null) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(continuationToken)
                            .build();
                }
                listResponse = s3Client.listObjectsV2(listRequest);
                if (listResponse.hasContents()) {
                    for (S3Object s3Object : listResponse.contents()) {
                        String oldObjectKey = s3Object.key();
                        String newObjectKey = newKey + oldObjectKey.substring(oldKey.length());
                        // Copy object
                        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                                .sourceBucket(bucketName)
                                .sourceKey(oldObjectKey)
                                .destinationBucket(bucketName)
                                .destinationKey(newObjectKey)
                                .build();
                        s3Client.copyObject(copyRequest);
                        keysToDelete.add(oldObjectKey);
                    }
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (listResponse.isTruncated());
            // Delete all old objects
            for (String keyToDelete : keysToDelete) {
                removeFile(keyToDelete);
            }
            log.info("Folder renamed from {} to {}", oldKey, newKey);
            return true;
        } catch (S3Exception e) {
            log.error("Error renaming folder: {} -> {}", oldKey, newKey, e);
            return false;
        }
    }
    @Override
    public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) {
        try {
            open();
            String targetKey = getS3Key(dir);
            if (!targetKey.isEmpty() && !targetKey.endsWith("/")) {
                targetKey += "/";
            }
            for (String filePath : filesToCopy) {
                String sourceKey = getS3Key(filePath);
                String fileName = sourceKey.substring(sourceKey.lastIndexOf('/') + 1);
                String destinationKey = targetKey + fileName;
                boolean isFolder = isFolder(sourceKey) || isFolderByListing(sourceKey);
                if (isFolder) {
                    if (copy) {
                        copyFolder(sourceKey, destinationKey);
                    } else {
                        renameFolder(sourceKey, destinationKey);
                    }
                } else {
                    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                            .sourceBucket(bucketName)
                            .sourceKey(sourceKey)
                            .destinationBucket(bucketName)
                            .destinationKey(destinationKey)
                            .build();
                    s3Client.copyObject(copyRequest);
                    if (!copy) {
                        // Move: delete source
                        removeFile(sourceKey);
                    }
                }
            }
            return true;
        } catch (S3Exception e) {
            log.error("Error moving/copying files in S3", e);
            return false;
        }
    }
    private void copyFolder(String sourceKey, String destinationKey) {
        try {
            if (!sourceKey.endsWith("/")) {
                sourceKey += "/";
            }
            if (!destinationKey.endsWith("/")) {
                destinationKey += "/";
            }
            // List all objects in the source folder
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(sourceKey)
                    .build();
            ListObjectsV2Response listResponse;
            String continuationToken = null;
            do {
                if (continuationToken != null) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(continuationToken)
                            .build();
                }
                listResponse = s3Client.listObjectsV2(listRequest);
                if (listResponse.hasContents()) {
                    for (S3Object s3Object : listResponse.contents()) {
                        String oldObjectKey = s3Object.key();
                        String newObjectKey = destinationKey + oldObjectKey.substring(sourceKey.length());
                        // Copy object
                        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                                .sourceBucket(bucketName)
                                .sourceKey(oldObjectKey)
                                .destinationBucket(bucketName)
                                .destinationKey(newObjectKey)
                                .build();
                        s3Client.copyObject(copyRequest);
                    }
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (listResponse.isTruncated());
        } catch (S3Exception e) {
            log.error("Error copying folder: {} -> {}", sourceKey, destinationKey, e);
            throw new EsupStockException("Error copying folder", e);
        }
    }
    @Override
    public DownloadFile getFile(String dir) {
        try {
            open();
            String s3Key = getS3Key(dir);
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
            GetObjectResponse objectResponse = response.response();
            String baseName = s3Key.substring(s3Key.lastIndexOf('/') + 1);
            String contentType = objectResponse.contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = JsTreeFile.getMimeType(baseName.toLowerCase());
            }
            long size = objectResponse.contentLength();
            // AWS SDK returns an InputStream that needs to stay open, so we wrap it
            return new DownloadFile(contentType, size, baseName, response);
        } catch (S3Exception e) {
            log.error("Error downloading file from S3", e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            throw new EsupStockException("S3 error", e);
        }
    }
    @Override
    public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {
        try {
            open();
            String parentKey = getS3Key(dir);
            if (!parentKey.isEmpty() && !parentKey.endsWith("/")) {
                parentKey += "/";
            }
            String s3Key = parentKey + filename;
            // Check if file exists
            boolean exists = objectExists(s3Key);
            if (exists) {
                switch (uploadOption) {
                    case ERROR:
                        throw new EsupStockFileExistException();
                    case OVERRIDE:
                        // Will overwrite
                        break;
                    case RENAME_NEW:
                        s3Key = parentKey + getUniqueFilename(filename, "-new-");
                        break;
                    case RENAME_OLD:
                        String oldKey = parentKey + getUniqueFilename(filename, "-old-");
                        renameObject(s3Key, oldKey);
                        break;
                }
            }
            // Read the entire input stream into memory
            // Note: For large files, consider using multipart upload
            byte[] content = inputStream.readAllBytes();
            String contentType = JsTreeFile.getMimeType(filename.toLowerCase());
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.info("File uploaded to S3: {}", s3Key);
            return true;
        } catch (S3Exception e) {
            log.error("Error uploading file to S3", e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            return false;
        } catch (IOException e) {
            log.error("Error reading input stream", e);
            return false;
        }
    }
    private boolean objectExists(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking if object exists: {}", s3Key, e);
            return false;
        }
    }
    private JsTreeFile createJsTreeFileForRoot(boolean folderDetails) {
        JsTreeFile file = new JsTreeFile("", "", "", "drive");
        file.setHidden(false);
        file.setReadable(true);
        file.setWriteable(true);
        if (folderDetails) {
            try {
                computeFolderDetails(file, basePath);
            } catch (Exception e) {
                log.error("Error computing folder details for root", e);
            }
        }
        return file;
    }
    private JsTreeFile createJsTreeFileForFolder(String path, boolean folderDetails) {
        String title = path.substring(path.lastIndexOf('/') + 1);
        String parent = path.substring(0, Math.max(0, path.lastIndexOf('/')));
        JsTreeFile file = new JsTreeFile(title, path, parent, "folder");
        file.setHidden(title.startsWith("."));
        file.setReadable(true);
        file.setWriteable(true);
        if (folderDetails) {
            try {
                String s3Key = getS3Key(path);
                if (!s3Key.endsWith("/")) {
                    s3Key += "/";
                }
                computeFolderDetails(file, s3Key);
            } catch (Exception e) {
                log.error("Error computing folder details for {}", path, e);
            }
        }
        return file;
    }
    private void computeFolderDetails(JsTreeFile file, String s3Prefix) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Prefix)
                    .delimiter("/")
                    .build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            long totalSize = 0;
            long fileCount = 0;
            long folderCount = 0;
            if (listResponse.hasCommonPrefixes()) {
                folderCount = listResponse.commonPrefixes().size();
            }
            if (listResponse.hasContents()) {
                for (S3Object s3Object : listResponse.contents()) {
                    if (!s3Object.key().equals(s3Prefix) && !s3Object.key().endsWith("/")) {
                        fileCount++;
                        totalSize += s3Object.size();
                    }
                }
            }
            file.setTotalSize(totalSize);
            file.setFileCount(fileCount);
            file.setFolderCount(folderCount);
        } catch (S3Exception e) {
            log.error("Error computing folder details", e);
        }
    }
    private JsTreeFile createJsTreeFileForS3Object(String path, S3Object s3Object, boolean fileDetails) {
        String title = path.substring(path.lastIndexOf('/') + 1);
        String parent = path.substring(0, Math.max(0, path.lastIndexOf('/')));
        JsTreeFile file = new JsTreeFile(title, path, parent, "file");
        file.setHidden(title.startsWith("."));
        String icon = resourceUtils.getIcon(title);
        file.setIcon(icon);
        file.setSize(s3Object.size());
        file.setOverSizeLimit(file.getSize() > resourceUtils.getSizeLimit(title));
        if (s3Object.lastModified() != null) {
            file.setLastModifiedTime(Date.from(s3Object.lastModified()));
        }
        file.setReadable(true);
        file.setWriteable(true);
        return file;
    }
    private JsTreeFile createJsTreeFileForObject(String path, HeadObjectResponse headResponse, boolean fileDetails) {
        String title = path.substring(path.lastIndexOf('/') + 1);
        String parent = path.substring(0, Math.max(0, path.lastIndexOf('/')));
        JsTreeFile file = new JsTreeFile(title, path, parent, "file");
        file.setHidden(title.startsWith("."));
        String icon = resourceUtils.getIcon(title);
        file.setIcon(icon);
        file.setSize(headResponse.contentLength());
        file.setOverSizeLimit(file.getSize() > resourceUtils.getSizeLimit(title));
        if (headResponse.lastModified() != null) {
            file.setLastModifiedTime(Date.from(headResponse.lastModified()));
        }
        file.setReadable(true);
        file.setWriteable(true);
        return file;
    }
    @Override
    public boolean supportIntraCopyPast() {
        return true;
    }

    @Override
    public boolean supportIntraCutPast() {
        return true;
    }

    /**
     * Initialize the S3 presigner if needed and not already initialized
     */
    private void initializePresigner() {
        if (s3Presigner == null && presignedUrlsEnabled) {
            try {
                S3Presigner.Builder presignerBuilder = S3Presigner.builder();

                // Configure credentials
                if (userAuthenticatorService != null) {
                    UserPassword userPassword = userAuthenticatorService.getUserPassword();
                    AwsBasicCredentials credentials = AwsBasicCredentials.create(
                            userPassword.getUsername(),
                            userPassword.getPassword()
                    );
                    presignerBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
                }

                // Configure region
                if (region != null && !region.isEmpty()) {
                    presignerBuilder.region(Region.of(region));
                }

                // Configure custom endpoint (for MinIO, Ceph, etc.)
                if (uri != null && !uri.isEmpty()) {
                    presignerBuilder.endpointOverride(URI.create(uri));
                }

                // For path-style access (MinIO), we need to configure the service configuration
                // The presigner will use path-style URLs when the endpoint is overridden and
                // the endpoint is not an AWS S3 endpoint
                if (pathStyleAccessEnabled) {
                    presignerBuilder.serviceConfiguration(
                        software.amazon.awssdk.services.s3.S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build()
                    );
                    log.debug("Presigner configured with pathStyleAccessEnabled=true");
                }

                s3Presigner = presignerBuilder.build();
                log.info("S3 Presigner initialized successfully with endpoint: {}, pathStyle: {}", uri, pathStyleAccessEnabled);
            } catch (Exception e) {
                log.error("Error initializing S3 presigner", e);
                throw new EsupStockException("S3 presigner initialization error", e);
            }
        }
    }

    @Override
    public boolean supportsPresignedUrls() {
        return presignedUrlsEnabled;
    }

    @Override
    public PresignedUrl getPresignedDownloadUrl(String path) {
        if (!presignedUrlsEnabled) {
            log.debug("Presigned URLs are not enabled");
            return null;
        }

        try {
            open();
            initializePresigner();

            String s3Key = getS3Key(path);
            String filename = s3Key.substring(s3Key.lastIndexOf('/') + 1);

            // Create GetObject request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // Create presign request with expiration
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // Generate presigned URL
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            String urlString = presignedRequest.url().toString();


            PresignedUrl result = new PresignedUrl(
                    urlString,
                    presignedRequest.expiration(),
                    "GET",
                    filename
            );

            log.info("Generated presigned download URL for {} (expires in {} minutes): {}", path, presignedUrlExpirationMinutes, urlString);
            return result;
        } catch (S3Exception e) {
            log.error("Error generating presigned download URL for {}", path, e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            throw new EsupStockException("S3 error generating presigned URL", e);
        } catch (Exception e) {
            log.error("Error generating presigned download URL", e);
            throw new EsupStockException("Error generating presigned download URL", e);
        }
    }

    @Override
    public PresignedUrl getPresignedUploadUrl(String path, String filename) {
        if (!presignedUrlsEnabled) {
            log.debug("Presigned URLs are not enabled");
            return null;
        }

        try {
            open();
            initializePresigner();

            String parentKey = getS3Key(path);
            if (!parentKey.isEmpty() && !parentKey.endsWith("/")) {
                parentKey += "/";
            }
            String s3Key = parentKey + filename;

            // Determine content type
            String contentType = JsTreeFile.getMimeType(filename.toLowerCase());

            // Create PutObject request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            // Create presign request with expiration
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Generate presigned URL
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            String urlString = presignedRequest.url().toString();


            PresignedUrl result = new PresignedUrl(
                    urlString,
                    presignedRequest.expiration(),
                    "PUT",
                    filename
            );

            log.info("Generated presigned upload URL for {} (expires in {} minutes): {}", s3Key, presignedUrlExpirationMinutes, urlString);
            return result;
        } catch (S3Exception e) {
            log.error("Error generating presigned upload URL for {}/{}", path, filename, e);
            if (e.statusCode() == 403) {
                throw new EsupStockPermissionDeniedException(e);
            }
            throw new EsupStockException("S3 error generating presigned URL", e);
        } catch (Exception e) {
            log.error("Error generating presigned upload URL", e);
            throw new EsupStockException("Error generating presigned upload URL", e);
        }
    }
}
