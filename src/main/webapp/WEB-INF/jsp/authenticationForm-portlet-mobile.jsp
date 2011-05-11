<%--

    Copyright (C) 2011 Esup Portail http://www.esup-portail.org
    Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
    @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
    @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
    @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>

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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='portlet' uri="http://java.sun.com/portlet" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<html>

  <head>
    <title>
      ENT Mobile
    </title>
    <link 
        rel="stylesheet"
        href="/esup-portlet-stockage/css/esup-stock-mobile.css"
        type="text/css"
        media="screen, projection"/>



  </head>


  <body up style="fl-theme-iphone">


    <portlet:actionURL var="authenticationFormMobile">
      <portlet:param name="action" value="formAuthenticationMobile"/>
      <portlet:param name="dir" value="${currentDir}"/>
    </portlet:actionURL>


    <div class="esupstock">

      <div class="portlet-section">

        <div class="portlet-section-body">


          <div id="info-toolbar">
            <c:if test="${!empty msg}">
              <spring:message code="${msg}"/>
            </c:if>
          </div>


          <form:form method="post" id="authenticationForm" action="${authenticationFormMobile}">

            <input type="hidden" id="currentDir" value="${currentDir}"/>

            <label for="username">
              <spring:message code="auth.username"/>
            </label>
            <input type="text" name="username" value="${username}"/>

            <label for="password">
              <spring:message code="auth.password"/>
            </label>
            <input type="password" name="password" value="${password}"/>

            <input type="submit" id="submit" value="<spring:message code="auth.submit"/>"/>

            <a 
                href="<portlet:renderURL><portlet:param name="action" value="browseMobile"/><portlet:param name="dir" value="${parentDir}"/></portlet:renderURL>">

              <spring:message code="auth.cancel"/>
            </a>

          </form:form>

        </div>

      </div>

    </div>

  </body>

</html>



