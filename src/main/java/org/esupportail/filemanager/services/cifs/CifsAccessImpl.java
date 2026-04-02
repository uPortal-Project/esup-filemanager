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
package org.esupportail.filemanager.services.cifs;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import jakarta.annotation.Resource;
import org.esupportail.filemanager.beans.DownloadFile;
import org.esupportail.filemanager.beans.JsTreeFile;
import org.esupportail.filemanager.beans.UploadActionType;
import org.esupportail.filemanager.beans.UserPassword;
import org.esupportail.filemanager.exceptions.EsupStockException;
import org.esupportail.filemanager.exceptions.EsupStockFileExistException;
import org.esupportail.filemanager.services.FsAccess;
import org.esupportail.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class CifsAccessImpl extends FsAccess implements DisposableBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CifsAccessImpl.class);

    // -----------------------------------------------------------------------
    // SMB connection state
    // -----------------------------------------------------------------------

    private SMBClient smbClient;
    private Connection connection;
    private Session session;
    private DiskShare diskShare;

    /** Parsed from the URI (smb://host[:port]/share[/basePath]) */
    private String smbHost;
    private int smbPort = SMBClient.DEFAULT_PORT;
    private String smbShareName;
    /** Base path inside the share (uses backslash separator, may be empty). */
    private String smbBasePath;

    // -----------------------------------------------------------------------
    // Spring wiring
    // -----------------------------------------------------------------------

    @Resource
    private ResourceUtils resourceUtils;

    public void setResourceUtils(ResourceUtils resourceUtils) {
        this.resourceUtils = resourceUtils;
    }

    // -----------------------------------------------------------------------
    // FsAccess overrides
    // -----------------------------------------------------------------------

    @Override
    public String getConnectionType() {
        return "CIFS/SMB";
    }

    // -----------------------------------------------------------------------
    // URI parsing helpers
    // -----------------------------------------------------------------------

    /**
     * Parses the inherited {@link #uri} field into {@link #smbHost},
     * {@link #smbPort}, {@link #smbShareName} and {@link #smbBasePath}.
     * <p>
     * Expected format: {@code smb://hostname[:port]/sharename[/optional/base/path]}
     */
    private void parseUri() {
        String u = uri;
        if (u.startsWith("smb://")) {
            u = u.substring(6);
        }
        // Strip trailing slashes
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }

        int firstSlash = u.indexOf('/');
        String hostPart;
        String rest;
        if (firstSlash < 0) {
            hostPart = u;
            smbShareName = "";
            smbBasePath = "";
        } else {
            hostPart = u.substring(0, firstSlash);
            rest = u.substring(firstSlash + 1);
            int secondSlash = rest.indexOf('/');
            if (secondSlash < 0) {
                smbShareName = rest;
                smbBasePath = "";
            } else {
                smbShareName = rest.substring(0, secondSlash);
                smbBasePath = rest.substring(secondSlash + 1).replace("/", "\\");
            }
        }

        // Separate host and optional port
        int colonIdx = hostPart.lastIndexOf(':');
        if (colonIdx >= 0) {
            smbHost = hostPart.substring(0, colonIdx);
            try {
                smbPort = Integer.parseInt(hostPart.substring(colonIdx + 1));
            } catch (NumberFormatException e) {
                log.warn("Unable to parse SMB port from URI '{}', using default 445", uri);
                smbHost = hostPart;
                smbPort = SMBClient.DEFAULT_PORT;
            }
        } else {
            smbHost = hostPart;
            smbPort = SMBClient.DEFAULT_PORT;
        }
    }

    /**
     * Builds the SMB path (backslash-separated, relative to share root) by
     * combining the base path declared in the URI with the caller-supplied
     * relative path (which may use forward or backslashes).
     */
    private String buildSmbPath(String relativePath) {
        String base = smbBasePath != null ? smbBasePath : "";
        if (relativePath == null || relativePath.isEmpty()) {
            return base;
        }
        String rel = relativePath.replace("/", "\\");
        if (base.isEmpty()) {
            return rel;
        }
        return base + "\\" + rel;
    }

    // -----------------------------------------------------------------------
    // Connection lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void open() {
        super.open();
        if (!isOpened()) {
            try {
                parseUri();
                smbClient = new SMBClient();
                connection = smbClient.connect(smbHost, smbPort);

                AuthenticationContext authContext;
                if (userAuthenticatorService != null) {
                    UserPassword up = userAuthenticatorService.getUserPassword();
                    String username = up.getUsername() != null ? up.getUsername() : "";
                    char[] password = up.getPassword() != null ? up.getPassword().toCharArray() : new char[0];
                    String domain = up.getDomain() != null ? up.getDomain() : "";
                    authContext = new AuthenticationContext(username, password, domain);
                } else {
                    authContext = AuthenticationContext.anonymous();
                }

                session = connection.authenticate(authContext);
                diskShare = (DiskShare) session.connectShare(smbShareName);
                notifyConnectionOpened();
                log.info("SMB connection opened: {}:{}/{}", smbHost, smbPort, smbShareName);
            } catch (IOException e) {
                throw new EsupStockException(e);
            }
        }
    }

    @Override
    public void close() {
        if (diskShare != null) {
            notifyConnectionClosed();
            try { diskShare.close(); } catch (Exception e) { log.warn("Error closing disk share", e); }
            diskShare = null;
        }
        if (session != null) {
            try { session.close(); } catch (Exception e) { log.warn("Error closing SMB session", e); }
            session = null;
        }
        if (connection != null) {
            try { connection.close(); } catch (Exception e) { log.warn("Error closing SMB connection", e); }
            connection = null;
        }
        if (smbClient != null) {
            try { smbClient.close(); } catch (Exception e) { log.warn("Error closing SMB client", e); }
            smbClient = null;
        }
    }

    @Override
    protected boolean isOpened() {
        return diskShare != null && diskShare.isConnected();
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

    // -----------------------------------------------------------------------
    // Directory / file metadata helpers
    // -----------------------------------------------------------------------

    /** Returns {@code true} if the file attribute long has the DIRECTORY bit set. */
    private static boolean isDirectory(long fileAttributes) {
        return (fileAttributes & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0;
    }

    /** Converts a smbj {@link FileTime} to a {@link Date}, or {@code null} if unset. */
    private static Date toDate(FileTime ft) {
        if (ft == null || ft.getWindowsTimeStamp() == 0) {
            return null;
        }
        long epochMillis = ft.toEpochMillis();
        return epochMillis > 0 ? new Date(epochMillis) : null;
    }

    /**
     * Builds a {@link JsTreeFile} for the entry at {@code smbPath} on the share.
     * When {@code smbPath} is empty the method returns the root ("drive") node.
     */
    private JsTreeFile smbPathAsJsTreeFile(String smbPath, String relativePath,
                                            boolean folderDetails, boolean fileDetails) {
        if (smbPath.isEmpty()) {
            // Root of the configured share/base-path → behave as a "drive" node
            JsTreeFile root = new JsTreeFile("", "", "", "drive");
            if (folderDetails) {
                populateFolderDetails(root, smbPath);
            }
            return root;
        }

        try {
            FileAllInformation info = diskShare.getFileInformation(smbPath);
            boolean dir = isDirectory(info.getBasicInformation().getFileAttributes());
            String type = dir ? "folder" : "file";

            // Title = last path component
            String title = smbPath.contains("\\")
                    ? smbPath.substring(smbPath.lastIndexOf('\\') + 1)
                    : smbPath;

            String parentRelPath = relativePath.contains("/")
                    ? relativePath.substring(0, relativePath.lastIndexOf('/'))
                    : "";

            JsTreeFile file = new JsTreeFile(title, relativePath, parentRelPath, type);

            if (!dir && resourceUtils != null) {
                file.setIcon(resourceUtils.getIcon(title));
                long size = info.getStandardInformation().getEndOfFile();
                file.setSize(size);
                file.setOverSizeLimit(size > resourceUtils.getSizeLimit(title));
            }

            Date lastModified = toDate(info.getBasicInformation().getLastWriteTime());
            if (lastModified != null) {
                file.setLastModifiedTime(lastModified);
            }

            if (folderDetails && dir) {
                populateFolderDetails(file, smbPath);
            }

            return file;
        } catch (SMBApiException e) {
            throw new EsupStockException(e);
        }
    }

    /** Counts children and total size for a directory node. */
    private void populateFolderDetails(JsTreeFile file, String smbPath) {
        try {
            List<FileIdBothDirectoryInformation> children = diskShare.list(smbPath);
            long totalSize = 0, fileCount = 0, folderCount = 0;
            for (FileIdBothDirectoryInformation child : children) {
                String name = child.getFileName();
                if (".".equals(name) || "..".equals(name)) continue;
                if (isDirectory(child.getFileAttributes())) {
                    folderCount++;
                } else {
                    fileCount++;
                    totalSize += child.getEndOfFile();
                }
            }
            file.setTotalSize(totalSize);
            file.setFileCount(fileCount);
            file.setFolderCount(folderCount);
        } catch (Exception e) {
            log.warn("Error computing folder details for '{}': {}", smbPath, e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // FsAccess abstract methods
    // -----------------------------------------------------------------------

    @Override
    public JsTreeFile get(String path, boolean folderDetails, boolean fileDetails) {
        this.open();
        try {
            return smbPathAsJsTreeFile(buildSmbPath(path), path, folderDetails, fileDetails);
        } catch (SMBApiException e) {
            throw new EsupStockException(e);
        }
    }

    @Override
    public List<JsTreeFile> getChildren(String path) {
        this.open();
        List<JsTreeFile> files = new ArrayList<>();
        try {
            String smbPath = buildSmbPath(path);
            List<FileIdBothDirectoryInformation> list = diskShare.list(smbPath);
            for (FileIdBothDirectoryInformation entry : list) {
                String name = entry.getFileName();
                if (".".equals(name) || "..".equals(name)) continue;
                // Skip hidden files (starting with a dot, Unix convention)
                if (name.startsWith(".")) continue;

                boolean dir = isDirectory(entry.getFileAttributes());
                String type = dir ? "folder" : "file";
                String childRelPath = (path == null || path.isEmpty()) ? name : path + "/" + name;
                String parentRelPath = path != null ? path : "";

                JsTreeFile jsFile = new JsTreeFile(name, childRelPath, parentRelPath, type);

                if (!dir && resourceUtils != null) {
                    jsFile.setIcon(resourceUtils.getIcon(name));
                    long size = entry.getEndOfFile();
                    jsFile.setSize(size);
                    jsFile.setOverSizeLimit(size > resourceUtils.getSizeLimit(name));
                }

                Date lastModified = toDate(entry.getLastWriteTime());
                if (lastModified != null) {
                    jsFile.setLastModifiedTime(lastModified);
                }

                files.add(jsFile);
            }
        } catch (SMBApiException e) {
            throw new EsupStockException(e);
        }
        return files;
    }

    @Override
    public boolean remove(String path) {
        this.open();
        try {
            String smbPath = buildSmbPath(path);
            if (diskShare.folderExists(smbPath)) {
                diskShare.rmdir(smbPath, true);
            } else {
                diskShare.rm(smbPath);
            }
            log.debug("Removed SMB entry '{}'", path);
            return true;
        } catch (Exception e) {
            log.warn("Cannot remove '{}': {}", path, e.getMessage());
            return false;
        }
    }

    @Override
    public String createFile(String parentPath, String title, String type) {
        this.open();
        try {
            String parentSmbPath = buildSmbPath(parentPath);
            String newSmbPath = parentSmbPath.isEmpty() ? title : parentSmbPath + "\\" + title;
            String newRelPath = (parentPath == null || parentPath.isEmpty()) ? title : parentPath + "/" + title;

            if ("folder".equals(type)) {
                diskShare.mkdir(newSmbPath);
                log.info("SMB folder '{}' created", title);
            } else {
                try (com.hierynomus.smbj.share.File f = diskShare.openFile(
                        newSmbPath,
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ, SMB2ShareAccess.FILE_SHARE_WRITE),
                        SMB2CreateDisposition.FILE_CREATE,
                        null)) {
                    // empty file created on close
                }
                log.info("SMB file '{}' created", title);
            }
            return newRelPath;
        } catch (Exception e) {
            log.warn("Cannot create '{}': {}", title, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean renameFile(String path, String title) {
        this.open();
        try {
            String smbPath = buildSmbPath(path);
            String parentSmbPath = smbPath.contains("\\")
                    ? smbPath.substring(0, smbPath.lastIndexOf('\\'))
                    : "";
            String newSmbPath = parentSmbPath.isEmpty() ? title : parentSmbPath + "\\" + title;

            EnumSet<AccessMask> accessMask = EnumSet.of(AccessMask.GENERIC_ALL);
            EnumSet<SMB2ShareAccess> shareAccess = EnumSet.of(
                    SMB2ShareAccess.FILE_SHARE_DELETE,
                    SMB2ShareAccess.FILE_SHARE_READ,
                    SMB2ShareAccess.FILE_SHARE_WRITE);

            if (diskShare.folderExists(smbPath)) {
                try (Directory dir = diskShare.openDirectory(
                        smbPath, accessMask, null, shareAccess,
                        SMB2CreateDisposition.FILE_OPEN, null)) {
                    dir.rename(newSmbPath);
                }
            } else {
                try (com.hierynomus.smbj.share.File file = diskShare.openFile(
                        smbPath, accessMask, null, shareAccess,
                        SMB2CreateDisposition.FILE_OPEN, null)) {
                    file.rename(newSmbPath);
                }
            }
            log.debug("Renamed SMB '{}' -> '{}'", path, title);
            return true;
        } catch (Exception e) {
            log.warn("Cannot rename '{}' to '{}': {}", path, title, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) {
        this.open();
        try {
            String dirSmbPath = buildSmbPath(dir);
            for (String srcRelPath : filesToCopy) {
                String srcSmbPath = buildSmbPath(srcRelPath);
                String name = srcSmbPath.contains("\\")
                        ? srcSmbPath.substring(srcSmbPath.lastIndexOf('\\') + 1)
                        : srcSmbPath;
                String destSmbPath = dirSmbPath.isEmpty() ? name : dirSmbPath + "\\" + name;

                if (copy) {
                    copySmbEntry(srcSmbPath, destSmbPath);
                } else {
                    // Move = rename to destination
                    EnumSet<AccessMask> accessMask = EnumSet.of(AccessMask.GENERIC_ALL);
                    EnumSet<SMB2ShareAccess> shareAccess = EnumSet.of(
                            SMB2ShareAccess.FILE_SHARE_DELETE,
                            SMB2ShareAccess.FILE_SHARE_READ,
                            SMB2ShareAccess.FILE_SHARE_WRITE);

                    if (diskShare.folderExists(srcSmbPath)) {
                        try (Directory d = diskShare.openDirectory(
                                srcSmbPath, accessMask, null, shareAccess,
                                SMB2CreateDisposition.FILE_OPEN, null)) {
                            d.rename(destSmbPath);
                        }
                    } else {
                        try (com.hierynomus.smbj.share.File f = diskShare.openFile(
                                srcSmbPath, accessMask, null, shareAccess,
                                SMB2CreateDisposition.FILE_OPEN, null)) {
                            f.rename(destSmbPath);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Cannot move/copy files into '{}': {}", dir, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Recursively copies an SMB entry (file or folder) to a new path on the same share.
     */
    private void copySmbEntry(String srcPath, String destPath) throws IOException {
        if (diskShare.folderExists(srcPath)) {
            diskShare.mkdir(destPath);
            List<FileIdBothDirectoryInformation> children = diskShare.list(srcPath);
            for (FileIdBothDirectoryInformation child : children) {
                String name = child.getFileName();
                if (".".equals(name) || "..".equals(name)) continue;
                copySmbEntry(srcPath + "\\" + name, destPath + "\\" + name);
            }
        } else {
            EnumSet<AccessMask> readAccess = EnumSet.of(AccessMask.GENERIC_READ);
            EnumSet<AccessMask> writeAccess = EnumSet.of(AccessMask.GENERIC_WRITE);
            EnumSet<SMB2ShareAccess> shareAccess = EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ);

            com.hierynomus.smbj.share.File src = diskShare.openFile(
                    srcPath, readAccess, null, shareAccess,
                    SMB2CreateDisposition.FILE_OPEN, null);
            com.hierynomus.smbj.share.File dest = diskShare.openFile(
                    destPath, writeAccess,
                    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    shareAccess,
                    SMB2CreateDisposition.FILE_CREATE, null);
            try {
                FileCopyUtils.copy(src.getInputStream(), dest.getOutputStream());
            } finally {
                try { dest.close(); } catch (Exception ignored) { }
                try { src.close(); } catch (Exception ignored) { }
            }
        }
    }

    @Override
    public DownloadFile getFile(String dir) {
        this.open();
        try {
            String smbPath = buildSmbPath(dir);
            String baseName = smbPath.contains("\\")
                    ? smbPath.substring(smbPath.lastIndexOf('\\') + 1)
                    : smbPath;
            String contentType = JsTreeFile.getMimeType(baseName.toLowerCase());

            // Keep the File open – wrap InputStream so the File is closed when the stream is closed
            com.hierynomus.smbj.share.File smbFile = diskShare.openFile(
                    smbPath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ, SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_OPEN,
                    null);

            long size = smbFile.getFileInformation(FileAllInformation.class)
                    .getStandardInformation().getEndOfFile();
            InputStream inputStream = new SmbFileInputStream(smbFile);
            return new DownloadFile(contentType, size, baseName, inputStream);
        } catch (Exception e) {
            log.warn("Cannot download file '{}': {}", dir, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {
        this.open();
        boolean success = false;
        String destSmbPath = null;

        try {
            String dirSmbPath = buildSmbPath(dir);
            destSmbPath = dirSmbPath.isEmpty() ? filename : dirSmbPath + "\\" + filename;

            if (diskShare.fileExists(destSmbPath)) {
                switch (uploadOption) {
                    case ERROR:
                        throw new EsupStockFileExistException();
                    case OVERRIDE:
                        diskShare.rm(destSmbPath);
                        break;
                    case RENAME_NEW:
                        String newName = getUniqueFilename(filename, "-new-");
                        destSmbPath = dirSmbPath.isEmpty() ? newName : dirSmbPath + "\\" + newName;
                        break;
                    case RENAME_OLD:
                        String oldName = getUniqueFilename(filename, "-old-");
                        String oldDestPath = dirSmbPath.isEmpty() ? oldName : dirSmbPath + "\\" + oldName;
                        try (com.hierynomus.smbj.share.File oldFile = diskShare.openFile(
                                destSmbPath,
                                EnumSet.of(AccessMask.GENERIC_ALL),
                                null,
                                EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE,
                                        SMB2ShareAccess.FILE_SHARE_READ,
                                        SMB2ShareAccess.FILE_SHARE_WRITE),
                                SMB2CreateDisposition.FILE_OPEN,
                                null)) {
                            oldFile.rename(oldDestPath);
                        }
                        break;
                }
            }

            try (com.hierynomus.smbj.share.File newFile = diskShare.openFile(
                    destSmbPath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_CREATE,
                    null)) {
                try (OutputStream out = newFile.getOutputStream()) {
                    FileCopyUtils.copy(inputStream, out);
                }
            }
            success = true;

        } catch (EsupStockFileExistException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cannot upload file '{}': {}", filename, e.getMessage(), e);
        }

        if (!success && destSmbPath != null) {
            try {
                if (diskShare != null && diskShare.fileExists(destSmbPath)) {
                    diskShare.rm(destSmbPath);
                    log.debug("Deleted corrupted upload '{}'", destSmbPath);
                }
            } catch (Exception e) {
                log.debug("Cannot delete corrupted upload '{}': {}", destSmbPath, e.getMessage());
            }
        }

        return success;
    }

    // -----------------------------------------------------------------------
    // Inner helper – keeps the smbj File handle alive while the stream is read
    // -----------------------------------------------------------------------

    /**
     * Wraps the {@link InputStream} returned by a smbj {@link com.hierynomus.smbj.share.File}
     * and closes the underlying File handle when the stream itself is closed.
     */
    private static final class SmbFileInputStream extends InputStream {

        private final com.hierynomus.smbj.share.File smbFile;
        private final InputStream delegate;

        SmbFileInputStream(com.hierynomus.smbj.share.File smbFile) {
            this.smbFile = smbFile;
            this.delegate = smbFile.getInputStream();
        }

        @Override public int read() throws IOException { return delegate.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException { return delegate.read(b, off, len); }
        @Override public int available() throws IOException { return delegate.available(); }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                try { smbFile.close(); } catch (Exception ignored) { }
            }
        }
    }
}
