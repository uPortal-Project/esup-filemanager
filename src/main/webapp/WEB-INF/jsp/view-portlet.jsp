<%--

    Copyright (C) 2011 Esup Portail http://www.esup-portail.org
    Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
    Copyright (C) 2011 RECIA http://www.recia.fr
    @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
    @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
    @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
    @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
    @Contributor (C) 2011 BULL http://www.bull.fr

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>

<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='portlet' uri="http://java.sun.com/portlet_2_0"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<portlet:defineObjects />

<c:set var="n">
  <portlet:namespace />
</c:set>

  <!-- Framework CSS  GIP Recia  For a bug in Chrome, important to load css
    before javascript as some parts of JS Read the css files
    See http://api.jquery.com/ready/ for further explanation
    -->

    <link rel="stylesheet" href="/esup-filemanager/css/esup-stock.css" type="text/css" media="screen, projection">

    <link rel="stylesheet" href="/esup-filemanager/css/jquery-ui-1.8.15.custom.css" type="text/css"  media="screen, projection">
    <link rel="stylesheet" href="/esup-filemanager/css/jquery.contextMenu.css" type="text/css"  media="screen, projection">
    <link rel="stylesheet" href="/esup-filemanager/css/jquery.diaporama.css" type="text/css"  media="screen, projection">
    <link type="text/css" href="/esup-filemanager/css/jplayer.blue.monday.css" rel="stylesheet" />

    <portlet:resourceURL id="htmlFileTree" var="htmlFileTreeURL" />

    <script type="text/javascript">
      var fileuploadTemplate = '<spring:message code="fileupload.template"/>';
      var fileTemplate = '<spring:message code="fileupload.fileTemplate"/>';
      var sharedSessionId = '${sharedSessionId}';
      var useDoubleClick = '${useDoubleClick}';
      var useCursorWaitDialog = '${useCursorWaitDialog}';
      var defaultPath = '${defaultPath}';
      
      var htmlFileTreeURL = '${htmlFileTreeURL}';
    </script>

    <script type="text/javascript" src="/esup-filemanager/js/jquery-1.4.2.min.js">
    </script>
    <script type="text/javascript">
    var jQuery = $.noConflict(true);
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.cookie.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery-ui-1.8.15.custom.min.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.hotkeys.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.jstree.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.jplayer.min.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.contextMenu.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/jquery.jDiaporama.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/fileuploader.js">
    </script>
    <script type="text/javascript" src="/esup-filemanager/js/esup-stock.js">
    </script>


<%-- 
<div id="accessible-version">
  <a href="<portlet:renderURL> <portlet:param name="action" value="browseWai"/></portlet:renderURL>">
      <spring:message code="version.accessible"/>
  </a>
</div>
--%>

    <jsp:include page="body.jsp" />
