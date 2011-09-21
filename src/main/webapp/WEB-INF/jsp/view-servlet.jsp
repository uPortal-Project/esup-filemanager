<%--

    Copyright (C) 2011 Esup Portail http://www.esup-portail.org
    Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
    @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
    @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
    @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
    @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>

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
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="fr" lang="fr">

  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>
      esup-servlet-stockage
    </title>
    
    <!-- Framework CSS  GIP Recia  For a bug in Chrome, important to load css 
    before javascript as some parts of JS Read the css files 
    See http://api.jquery.com/ready/ for further explanation
    -->
    <link 
        rel="stylesheet"
        href="/esup-portlet-stockage/css/blueprint/screen.css"
        type="text/css"
        media="screen, projection">

    <link rel="stylesheet" href="/esup-portlet-stockage/css/blueprint/print.css" type="text/css" media="print">

    <link rel="stylesheet" href="/esup-portlet-stockage/css/esup-stock.css" type="text/css" media="screen, projection">
    
    <!-- GIP RECIA : Required css for the application and the jquery plugins -->
    <% if("@ENVIRONMENT@" == "recia") { %>
        <link rel="stylesheet" href="/esup-portlet-stockage/css/esup-stock-recia.css" type="text/css" media="screen, projection">
        <link rel="stylesheet" href="/esup-portlet-stockage/css/jquery-ui-1.8.15.custom.css" type="text/css"  media="screen, projection">
        <link rel="stylesheet" href="/esup-portlet-stockage/css/jquery.contextMenu.css" type="text/css"  media="screen, projection">
        <link rel="stylesheet" href="/esup-portlet-stockage/css/jquery.diaporama.css" type="text/css"  media="screen, projection">
        <link type="text/css" href="/esup-portlet-stockage/css/jplayer.blue.monday.css" rel="stylesheet" />        
    
    <% } %>

    <script type="text/javascript">
      var fileuploadTemplate = '<spring:message code="fileupload.template"/>';
      var fileTemplate = '<spring:message code="fileupload.fileTemplate"/>';
      var sharedSessionId = '${sharedSessionId}';
    </script>

    <!-- JQUERY -->
 
    <script type="text/javascript" src="/esup-portlet-stockage/js/jquery-1.4.2.min.js">
    </script> 
    <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.jstree.js">
    </script>
    <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.cookie.js">
    </script>
    <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.hotkeys.js">
    </script>

    <script type="text/javascript" src="/esup-portlet-stockage/js/fileuploader.js">
    </script>
    
    <!-- GIP RECIA : JQuery plugins and application javascript includes -->
    <% if("@ENVIRONMENT@" == "recia") { %>
    
     <script type="text/javascript" src="/esup-portlet-stockage/js/jquery-ui-1.8.15.custom.min.js">
      </script>
     
         <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock-recia.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock-recia-dragdrop.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock-recia-cutpaste.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock-recia-jstree.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock-recia-actions.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.jplayer.min.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.contextMenu.js">
        </script>
        <script type="text/javascript" src="/esup-portlet-stockage/js/jquery.jDiaporama.js">
        </script>        
        <script type="text/javascript" src="/esup-portlet-stockage/js/fileTree_recia.js">
        </script>
    <% } else {  %>
        <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock.js">
        </script>
    <% } %>
    


  </head>

  <body>
    <!-- GIP RECIA : Add token to choice environment -->
    <% if("@ENVIRONMENT@" == "recia") { %>
       <jsp:include page="body_recia.jsp" />
    <% } else {  %>
       <jsp:include page="body.jsp" /> 
    <% } %>
  </body>

</html>
