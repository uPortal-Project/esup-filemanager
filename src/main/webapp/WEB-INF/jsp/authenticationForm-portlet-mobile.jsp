<%--

    Licensed to EsupPortail under one or more contributor license
    agreements. See the NOTICE file distributed with this work for
    additional information regarding copyright ownership.

    EsupPortail licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at:

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
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<portlet:defineObjects />
<c:set var="n"><portlet:namespace /></c:set>
<div id="${n}EsupFilemanager" class="ui-content esupstock" role="main" data-role="content" data-theme="c">
    <portlet:actionURL var="authenticationFormMobile" escapeXml="false">
      <portlet:param name="action" value="formAuthenticationMobile"/>
      <portlet:param name="dir" value="${currentDir}"/>
    </portlet:actionURL>
    <div class="ui-body">
    	<form method="post" id="authenticationForm" action="${authenticationFormMobile}">
    		<input type="hidden" id="currentDir" value="${currentDir}"/>
    		<label for="${n}username"><spring:message code="auth.username" htmlEscape="true"/></label>
    		<input id="${n}username" type="text" name="username" value="${username}"/>
    		<label for="${n}password"><spring:message code="auth.password" htmlEscape="true"/></label>
    		<input id="${n}password" type="password" name="password" value="${password}"/>
    		<input data-theme="b" type="submit" value="<spring:message code="auth.submit" htmlEscape="true"/>"/>
    	</form>
    	<%-- cancel --%>
    	<portlet:renderURL var="cancelUrl" escapeXml="true">
    		<portlet:param name="action" value="browseMobile"/>
    		<portlet:param name="dir" value="${parentDir}"/>
    	</portlet:renderURL>
    	<a data-role="button" data-icon="delete" data-theme="e" href="${cancelUrl}"><spring:message code="auth.cancel" htmlEscape="true"/></a>
    </div>
    <%--
          <div id="info-toolbar">
            <c:if test="${!empty msg}">
              <spring:message code="${msg}"/>
            </c:if>
          </div>
         --%>
</div>



