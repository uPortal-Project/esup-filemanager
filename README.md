# ESUP File Manager

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.esupportail.portlet.filemanager/esup-filemanager/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.esupportail.portlet.filemanager/esup-filemanager)
[![build status](https://github.com/uPortal-Project/esup-filemanager/workflows/CI/badge.svg?branch=master)](https://github.com/uPortal-Project/esup-filemanager/actions)

> Esup File Manager allows users to perform file management on their home directories.

## Features

* full ajax (jquery)
* WAI User Interface
* mobile look for browsing files (JQuery Mobile)
* copy/cut/past, rename, create folder, upload/download files
* copy/cut/past inter-servers
* use apache commons vfs -> file systems supported are here : http://commons.apache.org/vfs/filesystems.html - uri like file:///home/bob works for example - ftp and sftp ok ...
* CIFS support (with JCIFS)
* Webdav support (with Sardine)
* CMIS support (Apache Chemistry).
* use spring v3, mvc, annotations, etc.
* allow to configure servers access with a configuration file
* 4 authentification mods : no authentification, authentification username/password given in config file, authentification username/password with user form, authentification proxy cas (tested with cassified sftp).

## Portlet Preferences

Even if the most part of configurations is set on drives.xml, you can also define some portlet preferences when registering/publishing an "Esup File Manager" Portlet.

* `defaultPortletView` can be equals to standard, wai or mobile
* `defaultPath` that's the default path which is opened when the user displays the portlet (if defaultPath is multi-valued, the first available is choosed).
* `contextToken` it allows to show only some drives (that have the same contextToken set up on drives.xml)
* `showHiddenFiles` to hide/show 'hidden files'
* `useDoubleClick` use of double click when browsing
* `useCursorWaitDialog` display a waiting dialog when loading

Some of this preferences can also be edited by user himself with the "edit portlet mode".
