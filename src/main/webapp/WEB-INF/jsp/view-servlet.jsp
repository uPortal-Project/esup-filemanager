<%--

    Copyright (C) 2010 Esup Portail http://www.esup-portail.org
    Copyright (C) 2010 UNR RUNN http://www.unr-runn.fr
    @Author (C) 2010 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2010 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>

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

    <script type="text/javascript">
      var fileuploadTemplate = '<spring:message code="fileupload.template"/>';
      var fileTemplate = '<spring:message code="fileupload.fileTemplate"/>';
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

    <script type="text/javascript" src="/esup-portlet-stockage/js/esup-stock.js">
    </script>

    <!-- Framework CSS -->
    <link 
        rel="stylesheet"
        href="/esup-portlet-stockage/css/blueprint/screen.css"
        type="text/css"
        media="screen, projection">

    <link rel="stylesheet" href="/esup-portlet-stockage/css/blueprint/print.css" type="text/css" media="print">

    <link rel="stylesheet" href="/esup-portlet-stockage/css/esup-stock.css" type="text/css" media="screen, projection">


  </head>

  <body>

    <jsp:include page="body.jsp" />

  </body>

</html>
