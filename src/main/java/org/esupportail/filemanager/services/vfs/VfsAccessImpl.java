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
package org.esupportail.filemanager.services.vfs;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import jakarta.annotation.Resource;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.esupportail.filemanager.beans.DownloadFile;
import org.esupportail.filemanager.beans.JsTreeFile;
import org.esupportail.filemanager.beans.UploadActionType;
import org.esupportail.filemanager.beans.UserPassword;
import org.esupportail.filemanager.exceptions.EsupStockException;
import org.esupportail.filemanager.exceptions.EsupStockFileExistException;
import org.esupportail.filemanager.exceptions.EsupStockLostSessionException;
import org.esupportail.filemanager.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.filemanager.services.FsAccess;
import org.esupportail.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class VfsAccessImpl extends FsAccess implements DisposableBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VfsAccessImpl.class);

    protected FileSystemManager fsManager;

    protected FileObject root;

    @Resource
    ResourceUtils resourceUtils;

    protected boolean sftpSetUserDirIsRoot = false;

    protected boolean strictHostKeyChecking = true;

    protected String ftpControlEncoding = "UTF-8";

    // we setup ftpPassiveMode to true by default ...
    protected boolean ftpPassiveMode = true;

    /** JSCH properties */
    protected Map<String, String> jschConfigProperties;

    public void setJschConfigProperties(Map<String, String> jschConfigProperties) {
        this.jschConfigProperties = jschConfigProperties;
    }

    public void setResourceUtils(ResourceUtils resourceUtils) {
        this.resourceUtils = resourceUtils;
    }

    public void setSftpSetUserDirIsRoot(boolean sftpSetUserDirIsRoot) {
        this.sftpSetUserDirIsRoot = sftpSetUserDirIsRoot;
    }

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public void setFtpPassiveMode(boolean ftpPassiveMode) {
        this.ftpPassiveMode = ftpPassiveMode;
    }

    @Override
    public String getConnectionType() {
        String u = getUri();
        if (u != null) {
            if (u.startsWith("sftp://"))  return "SFTP";
            if (u.startsWith("ftp://"))   return "FTP";
            if (u.startsWith("ftps://"))  return "FTPS";
            if (u.startsWith("file://"))  return "Local";
            if (u.startsWith("smb://"))   return "CIFS/SMB";
        }
        return "VFS";
    }

    @Override
    protected void open() {
        super.open();
        try {
            if(!isOpened()) {

                if (this.jschConfigProperties != null && !this.jschConfigProperties.isEmpty()) {
                    JSch.setConfig(new Hashtable<>(this.jschConfigProperties));
                }

                FileSystemOptions fsOptions = new FileSystemOptions();

                if ( ftpControlEncoding != null )
                    FtpFileSystemConfigBuilder.getInstance().setControlEncoding(fsOptions, ftpControlEncoding);

                if(sftpSetUserDirIsRoot) {
                    SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, true);
                    FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, true);
                } else {
                    SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, false);
                    FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, false);
                }

                if(!strictHostKeyChecking) {
                    SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
                }

                FtpFileSystemConfigBuilder.getInstance().setPassiveMode(fsOptions, ftpPassiveMode);

                if(userAuthenticatorService != null) {
                    UserAuthenticator userAuthenticator = null;
                    UserPassword userPassword = userAuthenticatorService.getUserPassword();
                    userAuthenticator = new StaticUserAuthenticator(userPassword.getDomain(), userPassword.getUsername(), userPassword.getPassword());
                    DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(fsOptions, userAuthenticator);
                }

                fsManager = VFS.getManager();
                root = fsManager.resolveFile(uri, fsOptions);
                notifyConnectionOpened();
            }
        } catch(FileSystemException fse) {
            throw new EsupStockException(fse);
        }
    }

    @Override
    public void close() {
        FileSystem fs = null;
        if(this.root != null) {
            notifyConnectionClosed();
            fs = this.root.getFileSystem();
            this.fsManager.closeFileSystem(fs);
            this.root = null;
        }
    }

    public void destroy() throws Exception {
        this.close();
    }

    @Override
    protected boolean isOpened() {
        return (root != null);
    }

    private FileObject cd(String path) {
        try {
            // assure that it'as already opened
            this.open();

            FileObject returnValue = null;

            if (path == null || path.length() == 0) {
                returnValue = root;
            } else {
                returnValue = root.resolveFile(path);
            }

            //Added for GIP Recia : make sure that the file is up to date
            returnValue.refresh();
            return returnValue;
        } catch(FileSystemException fse) {
            throw new EsupStockException(fse);
        }
    }

    @Override
    public JsTreeFile get(String path, boolean folderDetails, boolean fileDetails) {
        try {
            FileObject resource = cd(path);
            return resourceAsJsTreeFile(resource, "", folderDetails, fileDetails, false);
        } catch(FileSystemException fse) {
            throw new EsupStockException(fse);
        }
    }

    @Override
    public List<JsTreeFile> getChildren(String path) {
        try {
            List<JsTreeFile> files = new ArrayList<JsTreeFile>();
            FileObject resource = cd(path);
            FileObject[] children = resource.getChildren();
            if(children != null)
                for(FileObject child: children)
                    if(!FileType.IMAGINARY.getName().equals(child.getType().getName()) && !this.isFileHidden(child))
                        files.add(resourceAsJsTreeFile(child, path, false, true, false));
            return files;
        } catch(FileSystemException fse) {
            Throwable cause = fse.getCause();
            if(cause != null && cause.getClass().equals(SftpException.class)) {
                SftpException sfe = (SftpException)cause;
                if(sfe.id == ChannelSftp.SSH_FX_PERMISSION_DENIED)
                    throw new EsupStockPermissionDeniedException(sfe);
            }
            if(cause != null && cause.getClass().equals(JSchException.class)) {
                if("session is down".equals(cause.getMessage())) {
                    log.info("Session is down, we close all so that we can try to reopen a connection");
                    throw new EsupStockLostSessionException((JSchException)cause);
                }
            }
            throw new EsupStockException(fse);
        }
    }



    private boolean isFileHidden(FileObject file) {
        boolean isHidden = false;
        // file.isHidden() works in current version of VFS (1.0) only for local file object :(
        if(file instanceof LocalFile) {
            try {
                isHidden = file.isHidden();
            } catch (FileSystemException e) {
                log.warn("Error on file.isHidden() method ...", e);
            }
        } else {
            // at the moment here we just check if the file begins with a dot
            // ... so it works just for unix files ...
            isHidden = file.getName().getBaseName().startsWith(".");
        }
        return isHidden;
    }

    private JsTreeFile resourceAsJsTreeFile(FileObject resource, String parent, boolean folderDetails, boolean fileDetails, boolean showHiddenFiles) throws FileSystemException {
        String lid = resource.getName().getPath();
        String rootPath = this.root.getName().getPath();
        // lid must be a relative path from rootPath
        if(lid.startsWith(rootPath))
            lid = lid.substring(rootPath.length());
        if(lid.startsWith("/"))
            lid = lid.substring(1);

        String title = "";
        String type = "drive";
        if(!"".equals(lid)) {
            type = resource.getType().getName();
            title = resource.getName().getBaseName();
        }
        JsTreeFile file = new JsTreeFile(title, lid, parent,  type);

        file.setHidden(this.isFileHidden(resource));

        if (FileType.FILE.getName().equals(type)) {
            String icon = resourceUtils.getIcon(title);
            file.setIcon(icon);
            file.setSize(resource.getContent().getSize());
            file.setOverSizeLimit(file.getSize() > resourceUtils
                    .getSizeLimit(title));
        }

        try {
            if(folderDetails && (FileType.FOLDER.getName().equals(type) || "drive".equals(type))) {
                if (resource.getChildren() != null) {
                    long totalSize = 0;
                    long fileCount = 0;
                    long folderCount = 0;
                    for (FileObject child : resource.getChildren()) {
                        if (showHiddenFiles || !this.isFileHidden(child)) {
                            if (FileType.FOLDER.getName().equals(child.getType().getName())) {
                                ++folderCount;
                            } else if (FileType.FILE.getName().equals(child.getType().getName())) {
                                ++fileCount;
                                totalSize += child.getContent().getSize();
                            }
                        }
                    }
                    file.setTotalSize(totalSize);
                    file.setFileCount(fileCount);
                    file.setFolderCount(folderCount);
                }
            }

            final Calendar date = Calendar.getInstance();
            date.setTimeInMillis(resource.getContent().getLastModifiedTime());
            // In order to have a readable date
            file.setLastModifiedTime(date.getTime());

            file.setReadable(resource.isReadable());
            file.setWriteable(resource.isWriteable());

        } catch(FileSystemException fse) {
            // we don't want that exception during retrieving details
            // of the folder breaks  all this method ...
            log.error("Exception during retrieveing details on {}... maybe broken symbolic links or whatever ...", lid, fse);
        }

        return file;
    }

    @Override
    public boolean remove(String path) {
        boolean success = false;
        FileObject file;
        try {
            file = cd(path);
            success = file.delete();
        } catch (FileSystemException e) {
            log.info("can't delete file because of FileSystemException:", e);
        }
        log.debug("remove file '{}': '{}'", path, success);
        return success;
    }

    @Override
    public String createFile(String parentPath, String title, String type) {
        try {
            FileObject parent = cd(parentPath);
            FileObject child = parent.resolveFile(title);
            if (!child.exists()) {
                if (FileType.FOLDER.getName().equals(type)) {
                    child.createFolder();
                    log.info("folder '{}' created", title);
                } else {
                    child.createFile();
                    log.info("file '{}' created", title);
                }
                return child.getName().getPath();
            } else {
                log.info("file '{}' already exists !", title);
            }
        } catch (FileSystemException e) {
            log.info("can't create file because of FileSystemException:", e);
        }
        return null;
    }

    @Override
    public boolean renameFile(String path, String title) {
        try {
            FileObject file = cd(path);
            FileObject newFile = file.getParent().resolveFile(title);
            if (!newFile.exists()) {
                file.moveTo(newFile);
                return true;
            } else {
                log.info("file '{}' already exists !", title);
            }
        } catch (FileSystemException e) {
            log.info("can't rename file because of FileSystemException:", e);
        }
        return false;
    }

    @Override
    public boolean moveCopyFilesIntoDirectory(String dir,
                                              List<String> filesToCopy, boolean copy) {
        try {
            FileObject folder = cd(dir);
            for (String fileToCopyPath : filesToCopy) {
                FileObject fileToCopy = cd(fileToCopyPath);
                FileObject newFile = folder.resolveFile(fileToCopy.getName()
                        .getBaseName());
                if (copy) {
                    newFile.copyFrom(fileToCopy, Selectors.SELECT_ALL);
                } else {
                    fileToCopy.moveTo(newFile);
                }

            }
            return true;
        } catch (FileSystemException e) {
            log.warn("can't move/copy file because of FileSystemException:", e);
        }
        return false;
    }

    @Override
    public DownloadFile getFile(String dir) {
        try {
            FileObject file = cd(dir);
            FileContent fc = file.getContent();
            long size = fc.getSize();
            String baseName = fc.getFile().getName().getBaseName();
            // fc.getContentInfo().getContentType() use URLConnection.getFileNameMap,
            // we prefer here to use our getMimeType : for Excel files and co
            // String contentType = fc.getContentInfo().getContentType();
            String contentType = JsTreeFile.getMimeType(baseName.toLowerCase());
            InputStream inputStream = fc.getInputStream();
            return new DownloadFile(contentType, size, baseName, inputStream);
        } catch (FileSystemException e) {
            log.warn("can't download file:", e);
        }
        return null;
    }

    @Override
    public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {

        boolean success = false;
        FileObject newFile = null;

        try {
            FileObject folder = cd(dir);
            newFile = folder.resolveFile(filename);
            if (newFile.exists()) {
                switch (uploadOption) {
                    case ERROR :
                        throw new EsupStockFileExistException();
                    case OVERRIDE :
                        newFile.delete();
                        break;
                    case RENAME_NEW :
                        newFile = folder.resolveFile(this.getUniqueFilename(filename, "-new-"));
                        break;
                    case RENAME_OLD :
                        newFile.moveTo(folder.resolveFile(this.getUniqueFilename(filename, "-old-")));
                        break;
                }
            }
            newFile.createFile();

            OutputStream outstr = newFile.getContent().getOutputStream();

            FileCopyUtils.copy(inputStream, outstr);

            success = true;
        } catch (FileSystemException e) {
            log.info("can't upload file:", e);
        } catch (IOException e) {
            log.warn("can't upload file:", e);
        }

        if(!success && newFile != null) {
            // problem when uploading the file -> the file uploaded is corrupted
            // best is to delete it
            try {
                newFile.delete();
                log.debug("delete corrupted file after bad upload ok ...");
            } catch(Exception e) {
                log.debug("can't delete corrupted file after bad upload: {}", e.getMessage(), e);
            }
        }

        return success;
    }

    /**
     * @param ftpControlEncoding the ftpControlEncoding to set
     */
    public void setFtpControlEncoding(String ftpControlEncoding) {
        this.ftpControlEncoding = ftpControlEncoding;
    }
}
