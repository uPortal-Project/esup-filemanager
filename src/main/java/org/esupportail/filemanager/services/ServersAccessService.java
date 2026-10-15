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
package org.esupportail.filemanager.services;

import org.apache.commons.vfs2.FileType;
import org.esupportail.filemanager.beans.*;
import org.esupportail.filemanager.crudlog.CrudLogLevel;
import org.esupportail.filemanager.exceptions.EsupStockException;
import org.esupportail.filemanager.crudlog.CrudLoggable;
import org.esupportail.filemanager.utils.PathEncodingUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service("serversAccess")
@Scope(value="session", proxyMode=ScopedProxyMode.INTERFACES)
public class ServersAccessService implements DisposableBean, IServersAccessService, InitializingBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServersAccessService.class);

    /** Size of zipping buffers: 128 kB. */
    protected static final int ZIP_BUFFER_SIZE = 131072;

    protected Map<String, FsAccess> servers = new HashMap<String, FsAccess>();

    @Autowired
    public void setServers(List<FsAccess> servers) {
        for(FsAccess server: servers) {
            this.servers.put(server.getDriveName(), server);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    //@Resource(name="drivesCategories")
    protected Map<String, DrivesCategory> drivesCategories;

    @Autowired
    protected PathEncodingUtils pathEncodingUtils;

    // With spring 4.3.x and prior a Map can't be loaded by @Autowired, but @Resource could work if jakarta annotation library is used
    @Override
    public void afterPropertiesSet() throws Exception {
        drivesCategories = applicationContext.getBean("drivesCategories", Map.class);
    }


    public void destroy() throws Exception {
        for(FsAccess server: this.servers.values()) {
            server.close();
        }
    }

    public FsAccess getFsAccess(String driveName) {
        if(driveName == null) {
            throw new IllegalArgumentException("Drive name is null");
        } else if(this.servers.containsKey(driveName) && this.servers.get(driveName).hasAccess()) {
            return this.servers.get(driveName);
        } else {
            log.error("pb : restrictedServers does not contain this required drive ?? : '{}'", driveName);
            throw new AccessDeniedException("Access to the requested drive is denied");
        }
    }

    protected List<FsAccess> getCategoryFsAccess(DrivesCategory dCategory) {
        List<FsAccess> drives = new ArrayList<FsAccess>();
        for(String driveName: dCategory.getDrives())
            if(this.servers.containsKey(driveName) && this.servers.get(driveName).hasAccess()) {
                drives.add(this.servers.get(driveName));
            }
        return drives;
    }

    @CrudLoggable(CrudLogLevel.DEBUG)
    public JsTreeFile get(String dir, boolean folderDetails, boolean fileDetails) {
        String category = getDriveCategory(dir);
        String driveName = getDrive(dir);
        if(category == null || category.length() == 0) {
            return getJsTreeFileRoot();
        } else if(driveName == null || driveName.length() == 0) {
            // get category
            DrivesCategory dCat = this.drivesCategories.get(category);
            JsTreeFile jsTreeFile = new JsTreeFile(category, "", "", "category");
            jsTreeFile.setIcon(dCat.getIcon());
            jsTreeFile.setCategory(category, dCat.getIcon());
            return jsTreeFile;
        } else {
            // get drive or folder or file
            String path = getLocalDir(dir);
            JsTreeFile jsTreeFile = this.getFsAccess(driveName).get(path, folderDetails, fileDetails);
            DrivesCategory dCat = this.drivesCategories.get(category);
            jsTreeFile.setCategory(category, dCat.getIcon());
            jsTreeFile.setDrive(driveName, this.getFsAccess(driveName).getIcon());
            if(jsTreeFile.getTitle().length() == 0) {
                // this the folder root == the drive
                jsTreeFile.setTitle(driveName);
                jsTreeFile.setIcon(this.getFsAccess(driveName).getIcon());
            }
            return jsTreeFile;
        }
    }

    @CrudLoggable(CrudLogLevel.DEBUG)
    public List<JsTreeFile> getChildren(String dir) {
        String category = getDriveCategory(dir);
        String driveName = getDrive(dir);
        DrivesCategory dCat = this.drivesCategories.get(category);
        if(category == null || category.length() == 0) {
            return getJsTreeFileRoots().get(0).getChildren();
        } else if(driveName == null || driveName.length() == 0) {
            // getChildren on a category -> list drives
            List<JsTreeFile> files = new ArrayList<JsTreeFile>();
            for(FsAccess drive: getCategoryFsAccess(dCat)) {
                JsTreeFile jsTreeFile = new JsTreeFile(drive.getDriveName(), "", "", "drive");
                jsTreeFile.setIcon(drive.getIcon());
                jsTreeFile.setCategory(category, dCat.getIcon());
                jsTreeFile.setDrive(drive.getDriveName(), drive.getIcon());
                files.add(jsTreeFile);
            }
            Collections.sort(files);
            return files;
        } else {
            // getChildren on a folder (or drive) -> get children on a fsAccess
            String path = getLocalDir(dir);
            List<JsTreeFile> files = this.getFsAccess(driveName).getChildren(path);
            for(JsTreeFile file: files) {
                file.setCategory(category, dCat.getIcon());
                file.setDrive(driveName, this.getFsAccess(driveName).getIcon());
            }
            Collections.sort(files);
            return files;
        }
    }

    @CrudLoggable(CrudLogLevel.DEBUG)
    public List<JsTreeFile> getFolderChildren(String dir
                                              ) {
        List<JsTreeFile> files = this.getChildren(dir);
        List<JsTreeFile> folders = new ArrayList<JsTreeFile>();
        for(JsTreeFile file: files) {
            if(!"file".equals(file.getType()))
                folders.add(file);
        }
        Collections.sort(folders);
        return folders;
    }

    @CrudLoggable(CrudLogLevel.INFO)
    public boolean remove(String dir) {
        return this.getFsAccess(getDrive(dir)).remove(getLocalDir(dir));
    }

    @CrudLoggable(CrudLogLevel.INFO)
    public String createFile(String parentDir, String title, String type) {
        if (!isSafeName(title)) {
            return null;
        }
        String drive = getDrive(parentDir);
        if(drive == null) {
            log.error("Can't create file/folder because we can't retrieve associated drive on this dir '{}'", parentDir);
            return null;
        }
        String parent = getLocalDir(parentDir);
        if (containsName(drive, parent, title)) {
            return null;
        }
        return this.getFsAccess(drive).createFile(parent, title, type);
    }

    @CrudLoggable(CrudLogLevel.INFO)
    public boolean renameFile(String dir, String title) {
        if (!isSafeName(title)) {
            return false;
        }
        String drive = getDrive(dir);
        if(drive == null) {
            log.error("Can't rename file/folder because we can't retrieve associated drive on this dir '{}'", dir);
            return false;
        }
        String local = getLocalDir(dir).replaceAll("/+$", "");
        String parent = local.contains("/") ? local.substring(0, local.lastIndexOf('/')) : "";
        if (containsName(drive, parent, title)) {
            return false;
        }
        return this.getFsAccess(drive).renameFile(getLocalDir(dir), title);
    }

    private boolean containsName(String drive, String parent, String title) {
        // Listings also expose implicit S3 directories that have no marker object.
        return this.getFsAccess(drive).getChildren(parent).stream().anyMatch(child ->
                "samba".equals(drive) ? child.getTitle().equalsIgnoreCase(title) : child.getTitle().equals(title));
    }

    private InputStream checkedDownload(DownloadFile file) {
        long expectedSize = file.getSize();
        return new FilterInputStream(file.getInputStream()) {
            private long received;

            private void validate(int count) throws IOException {
                if (count < 0 && expectedSize >= 0 && received != expectedSize) {
                    throw new EOFException("Source ended after " + received + " of " + expectedSize + " bytes");
                }
                if (count > 0) {
                    received += count;
                    if (expectedSize >= 0 && received > expectedSize) {
                        throw new IOException("Source size changed during transfer");
                    }
                }
            }

            @Override
            public int read() throws IOException {
                int value = in.read();
                validate(value < 0 ? -1 : 1);
                return value;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                int count = in.read(bytes, offset, length);
                validate(count);
                return count;
            }
        };
    }

    private boolean interMoveCopyFile(String newDir, String refDir, boolean copy) {
        try {
            JsTreeFile ref = this.get(refDir, false, false);
            if (ref == null || !("file".equals(ref.getType()) || "folder".equals(ref.getType()))) {
                return false;
            }
            boolean allIsOk = true;
            if ("file".equals(ref.getType())) {
                DownloadFile file = this.getFile(refDir);
                if (file == null) {
                    return false;
                }
                try (InputStream input = checkedDownload(file)) {
                    allIsOk = this.putFile(newDir, file.getBaseName(), input, UploadActionType.ERROR);
                }
            } else {
                String localDirParent = this.createFile(newDir, ref.getTitle(), ref.getType());
                if (localDirParent == null) {
                    log.info("Cannot transfer '{}' into '{}': destination directory already exists or is inaccessible", refDir, newDir);
                    return false;
                }
                String dirParent = JsTreeFile.ROOT_DRIVE.concat(getDriveCategory(newDir)).concat(JsTreeFile.DRIVE_PATH_SEPARATOR)
                        .concat(getDrive(newDir)).concat(JsTreeFile.DRIVE_PATH_SEPARATOR).concat(localDirParent);
                for (JsTreeFile child : this.getChildren(refDir)) {
                    allIsOk = allIsOk && this.interMoveCopyFile(dirParent, child.getPath(), copy);
                }
            }
            if (allIsOk && !copy) {
                allIsOk = this.remove(refDir);
            }
            return allIsOk;
        } catch (EsupStockException | IOException exception) {
            // A failed write must never proceed to source deletion or escape the business error response.
            log.warn("Cannot transfer '{}' into '{}': {}", refDir, newDir, exception.getMessage());
            return false;
        }
    }

    @CrudLoggable(CrudLogLevel.INFO)
    public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) {
        if (filesToCopy == null || filesToCopy.isEmpty()) {
            return false;
        }
        String driveName = getDrive(dir);
        if (driveName == null) {
            return false;
        }
        String destination = getLocalDir(dir).replaceAll("/+$", "");
        for (String source : filesToCopy) {
            if (driveName.equals(getDrive(source))) {
                String localSource = getLocalDir(source).replaceAll("/+$", "");
                String parent = localSource.contains("/") ? localSource.substring(0, localSource.lastIndexOf('/')) : "";
                // Reject no-op moves and recursive copies before any connector can mutate data.
                if (destination.equals(parent) || destination.equals(localSource) || destination.startsWith(localSource + "/")) {
                    return false;
                }
            }
        }
        try {
            JsTreeFile target = get(dir, false, false);
            if (target == null || !("folder".equals(target.getType()) || "drive".equals(target.getType()))) {
                return false;
            }
            Set<String> existingNames = new HashSet<>();
            for (JsTreeFile child : getChildren(dir)) {
                existingNames.add("samba".equals(driveName) ? child.getTitle().toLowerCase(Locale.ROOT) : child.getTitle());
            }
            for (String source : filesToCopy) {
                JsTreeFile resource = get(source, false, false);
                if (resource == null || !("file".equals(resource.getType()) || "folder".equals(resource.getType()))) {
                    return false;
                }
                String name = "samba".equals(driveName) ? resource.getTitle().toLowerCase(Locale.ROOT) : resource.getTitle();
                // Native connectors must honor the same no-overwrite contract as inter-drive transfers.
                if (existingNames.contains(name)) {
                    return false;
                }
                existingNames.add(name);
            }
        } catch (EsupStockException exception) {
            log.warn("Cannot validate transfer destination '{}': {}", dir, exception.getMessage());
            return false;
        }
        if(filesToCopy.stream().allMatch(source -> driveName.equals(getDrive(source))) &&
                ( (copy && this.getFsAccess(driveName).supportIntraCopyPast()) || (!copy && this.getFsAccess(driveName).supportIntraCutPast())) ) {
            return this.getFsAccess(driveName).moveCopyFilesIntoDirectory(getLocalDir(dir), getLocalDirs(filesToCopy), copy);
        } else {
            boolean allIsOk = true;
            for(String fileToCopy: filesToCopy) {
                boolean isOk = this.interMoveCopyFile(dir, fileToCopy, copy);
                allIsOk = allIsOk && isOk;
            }
            return allIsOk;
        }
    }

    @CrudLoggable(CrudLogLevel.DEBUG)
    public DownloadFile getFile(String dir) {
        return this.getFsAccess(getDrive(dir)).getFile(getLocalDir(dir));
    }

    @CrudLoggable(CrudLogLevel.INFO)
    public boolean  putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {
        if (!isSafeName(filename)) {
            return false;
        }
        return this.getFsAccess(getDrive(dir)).putFile(getLocalDir(dir), filename, inputStream, uploadOption);
    }

    public boolean fileExists(String dir, String filename) {
        return this.getFsAccess(getDrive(dir)).existsFile(getLocalDir(dir), filename);
    }

    public JsTreeFile getJsTreeFileRoot() {
        JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "", "root");
        jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
        return jsFileRoot;
    }

    public List<JsTreeFile> getJsTreeFileRoots() {
        JsTreeFile jsFileRoot = getJsTreeFileRoot();
        List<JsTreeFile> jsTreeFiles = new ArrayList<JsTreeFile>();
        for(String drivesCategoryName: this.drivesCategories.keySet()) {
            DrivesCategory category = this.drivesCategories.get(drivesCategoryName);
            if(!getCategoryFsAccess(category).isEmpty()) {
                JsTreeFile jFile = new JsTreeFile(drivesCategoryName, "", "", "category");
                jFile.setIcon(this.drivesCategories.get(drivesCategoryName).getIcon());
                jFile.setCategory(drivesCategoryName, this.drivesCategories.get(drivesCategoryName).getIcon());
                jFile.setChildren(this.getChildren(jFile.getPath()));
                jsTreeFiles.add(jFile);
            }
        }
        Collections.sort(jsTreeFiles);
        jsFileRoot.setChildren(jsTreeFiles);
        List<JsTreeFile> jsTreeFileRoots = new ArrayList<JsTreeFile>();
        jsTreeFileRoots.add(jsFileRoot);
        return jsTreeFileRoots;
    }

    public List<JsTreeFile> getJsTreeFileRoots(String dir) {

        JsTreeFile parentFile = null;

        List<JsTreeFile> rootAndDrivesAndCategories = this.getJsTreeFileRoots();
        JsTreeFile jFile = this.get(dir, false, false);

        //Iterator<String> parentsPathes = jFile.getParentsPathes().keySet().iterator();
        Iterator<String> parentsPathes = pathEncodingUtils.getParentsPathes(jFile.getPath(), null, null).keySet().iterator();
        String parentPath = parentsPathes.next();
        Assert.isTrue(JsTreeFile.ROOT_DRIVE.equals(parentPath), "ParentPath isn't the root path");

        if(!parentsPathes.hasNext())
            return rootAndDrivesAndCategories;

        parentPath = parentsPathes.next();
        for(JsTreeFile drive: rootAndDrivesAndCategories.get(0).getChildren()) {
            if(drive.getPath().equals(parentPath)) {
                parentFile = drive;
                break;
            }
        }

        if(!parentsPathes.hasNext())
            return rootAndDrivesAndCategories;

        parentPath = parentsPathes.next();
        for(JsTreeFile category: parentFile.getChildren()) {
            if(category.getPath().equals(parentPath)) {
                parentFile = category;
                break;
            }
        }

        while(parentPath != null) {
            List<JsTreeFile> folders = this.getFolderChildren(parentFile.getPath());
            parentFile.setChildren(folders);

            if(!parentsPathes.hasNext()) {
                parentPath = null;
            } else {
                parentPath = parentsPathes.next();
                for(JsTreeFile child: folders) {
                    if(child.getPath().equals(parentPath)) {
                        parentFile = child;
                        break;
                    }
                }
            }
        }
        return rootAndDrivesAndCategories;
    }

    public String getDriveCategory(String dir) {
        if(dir == null || dir.length() <= JsTreeFile.ROOT_DRIVE.length())
            return null;
        dir = dir.substring(JsTreeFile.ROOT_DRIVE.length());
        String[] driveAndDir = dir.split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
        return driveAndDir[0];
    }

    public String getDrive(String dir) {
        String drive = null;
        if(dir != null && dir.length() > JsTreeFile.ROOT_DRIVE.length()) {
            dir = dir.substring(JsTreeFile.ROOT_DRIVE.length());
            String[] driveAndDir = dir.split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
            if(driveAndDir.length > 1) {
                drive = driveAndDir[1];
            }
        } else {
            log.debug("Can't get drive because dir is null or too short: '{}'", dir);
        }
        return drive;
    }

    private boolean isSafeName(String name) {
        return name != null && !name.isBlank() && !".".equals(name) && !"..".equals(name)
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0
                && name.chars().noneMatch(Character::isISOControl);
    }

    private String getLocalDir(String dir) {
        if (dir == null || !dir.startsWith(JsTreeFile.ROOT_DRIVE)) {
            throw new AccessDeniedException("Invalid drive path");
        }
        String[] driveAndDir = dir.substring(JsTreeFile.ROOT_DRIVE.length()).split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
        String local = driveAndDir.length > 2 ? driveAndDir[2] : "";
        // Reject traversal before a protocol client can decode URI components or resolve an absolute path.
        String checked = local.replaceAll("(?i)%2e", ".").replaceAll("(?i)%2f", "/")
                .replaceAll("(?i)%5c", "/").replace('\\', '/');
        for (String segment : checked.split("/")) {
            if ("..".equals(segment) || ".".equals(segment)) {
                throw new AccessDeniedException("Relative path traversal is forbidden");
            }
        }
        if (checked.startsWith("/")) {
            throw new AccessDeniedException("Absolute storage paths are forbidden");
        }
        return local;
    }

    private List<String> getLocalDirs(List<String> dirs) {
        List<String> localDirs = new ArrayList<String>();
        for(String dir: dirs)
            localDirs.add(getLocalDir(dir));
        return localDirs;
    }

    @CrudLoggable(CrudLogLevel.DEBUG)
    public void writeZip(OutputStream destStream, List<String> dirs) throws IOException {

        ZipOutputStream out = new ZipOutputStream(destStream);
        final byte zippingBuffer[] = new byte[ZIP_BUFFER_SIZE];
        for(String dir: dirs) {
            this.addChildrensTozip(out, zippingBuffer, dir, "");
        }
        out.close();

    }

    private static String unAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }

    private void addChildrensTozip(ZipOutputStream out, byte[] zippingBuffer, String dir, String folder) throws IOException {
        JsTreeFile tFile = get(dir, false, false);
        if(FileType.FILE.getName().equals(tFile.getType())) {
            DownloadFile dFile = getFile(dir);

            //GIP Recia : In some cases (ie, file has NTFS security permissions set), the dFile may be Null.
            //So we must check for null in order to prevent a general catastrophe
            if (dFile == null) {
                log.warn("Download file is null! '{}'", dir);
                return;
            }
            String fileName =  unAccent(folder.concat(dFile.getBaseName()));

            //With java 7, encoding should be added to support special characters in the file names
            //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4244499
            out.putNextEntry(new ZipEntry(fileName));

            // MBD: this is a problem for large files, because IOUtils.toByteArray() copy all the file in memory
            //out.write(IOUtils.toByteArray(dFile.getInputStream()));
            int count;
            try (InputStream dFileInputStream = dFile.getInputStream()) {
                while ((count = dFileInputStream.read(zippingBuffer, 0, ZIP_BUFFER_SIZE)) != -1) {
                    out.write(zippingBuffer, 0, count);
                }
            }

            out.closeEntry();
        } else {
            folder = unAccent(folder.concat(tFile.getTitle()).concat("/"));
            //Added for GIP Recia : This creates an empty file with the same name as the directory but it allows
            //for zipping empty directories
            out.putNextEntry(new ZipEntry(folder));
            out.closeEntry();
            List<JsTreeFile> childrens = this.getChildren(dir);
            for(JsTreeFile child: childrens) {
                this.addChildrensTozip(out, zippingBuffer, child.getPath(), folder);
            }
        }
    }

    public boolean formAuthenticationRequired(String dir) {
        if(getDrive(dir) == null)
            return false;
        return this.getFsAccess(getDrive(dir)).formAuthenticationRequired();
    }

    public UserPassword getUserPassword(String dir) {
        if(getDrive(dir) == null)
            return null;
        return this.getFsAccess(getDrive(dir)).getUserPassword();
    }

    public boolean authenticate(String dir, String username, String password) {
        return this.getFsAccess(getDrive(dir)).authenticate(username, password);
    }

    public String getFirstAvailablePath(String[] prefsDefaultPathes) {
        String defaultPath = JsTreeFile.ROOT_DRIVE;
        Map<String, FsAccess> rServers = this.servers;
        for(String prefDefaultPath: prefsDefaultPathes) {
            String drive = getDrive(prefDefaultPath);
            if(rServers.get(drive) != null && rServers.get(drive).hasAccess()) {
                defaultPath = prefDefaultPath;
                break;
            }
        }
        return defaultPath;
    }

    public Quota getQuota(String path) {
        FsAccess access = this.getFsAccess(getDrive(path));
        Quota result = null;
        if ( access.isSupportQuota(getLocalDir(path)) ) {
            result = access.getQuota(getLocalDir(path));
        }
        return result;
    }

    @Override
    public boolean supportsPresignedUrls(String path) {
        String drive = getDrive(path);
        if (drive == null) {
            return false;
        }
        FsAccess access = this.getFsAccess(drive);
        return access != null && access.supportsPresignedUrls();
    }

    @Override
    public PresignedUrl getPresignedDownloadUrl(String path) {
        String drive = getDrive(path);
        if (drive == null) {
            log.warn("Cannot get presigned download URL: no drive found for path {}", path);
            return null;
        }
        FsAccess access = this.getFsAccess(drive);
        if (access == null) {
            log.warn("Cannot get presigned download URL: no FsAccess found for drive {}", drive);
            return null;
        }
        return access.getPresignedDownloadUrl(getLocalDir(path));
    }

    @Override
    public PresignedUrl getPresignedUploadUrl(String path, String filename) {
        String drive = getDrive(path);
        if (drive == null) {
            log.warn("Cannot get presigned upload URL: no drive found for path {}", path);
            return null;
        }
        FsAccess access = this.getFsAccess(drive);
        if (access == null) {
            log.warn("Cannot get presigned upload URL: no FsAccess found for drive {}", drive);
            return null;
        }
        return access.getPresignedUploadUrl(getLocalDir(path), filename);
    }
}
