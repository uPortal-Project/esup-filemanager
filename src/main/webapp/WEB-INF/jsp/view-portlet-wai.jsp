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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='portlet' uri="http://java.sun.com/portlet"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


<portlet:defineObjects />

<c:set var="n">
  <portlet:namespace />
</c:set>


<link rel="stylesheet" href="/esup-portlet-stockage/css/esup-stock-wai.css" type="text/css" media="screen, projection">


<portlet:actionURL var="formProcessWai">
  <portlet:param name="action" value="formProcessWai"/>
  <portlet:param name="dir" value="${currentDir}"/>
  <portlet:param name="sharedSessionId" value="${n}"/>
</portlet:actionURL>


<div class="esupstock">

  <div class="portlet-section">

    <div class="portlet-section-body">

      <form:form action="${formProcessWai}" method="post">

        <div id="info-toolbar">
          <c:if test="${!empty msg}">
            <spring:message code="${msg}"/>
          </c:if>
        </div>

        <%-- this block is used only when javascript is disabled --%>

        <div class="breadcrumbs">
          <c:forEach var="parent" items="${resource.parentsEncPathes}" varStatus="item">
            <c:set var="iconAlt">
            	<c:if test="${item.first}">/</c:if>
            </c:set>
            <c:choose>
              <c:when test="${item.last}">
                <img src="${resource.icon}" alt="${iconAlt}" />
                <span>
                  ${resource.title}
                </span>
              </c:when>
              <c:otherwise>
                <a href="<portlet:renderURL> <portlet:param name="action" value="browseWai"/> <portlet:param name="dir" value="${parent.key}"/></portlet:renderURL>">
			     <img src="${parent.value[1]}" alt="${iconAlt}" />
                  ${parent.value[0]}
                </a>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </div>


        <ul id="jqueryFileTreeWai">

          <c:forEach var="file" items="${files}">
            <li class="browserlist fl-container">
            <form:checkbox path="dirs" cssClass="browsercheck" value="${file.encPath}" />
            <c:choose>
              <c:when test="${'file' == file.type}">
                <img src="${file.icon}" alt="" />
                <a class="file" href="/esup-portlet-stockage/servlet-ajax/downloadFile?dir=${file.encPath}&sharedSessionId=${sharedSessionId}">
                  ${file.title}
                </a>
              </c:when>
              <c:otherwise>
                <img src="${file.icon}" alt="" />
                <a 
                    class="fileTreeRef"
                    href="<portlet:renderURL><portlet:param name="action" value="browseWai"/><portlet:param name="dir" value="${file.encPath}"/></portlet:renderURL>">
                  ${file.title}
                </a>
              </c:otherwise>
            </c:choose>
          </li>
        </c:forEach>

      </ul>

      <ul id="toolbar">
        <li class="toolbar-item">
        <input 
            type="submit"
            value="<spring:message code="toolbar.copy"/>"
            id="toolbar-copy-wai"
            name="prepareCopy"/>
      </li>
      <li class="toolbar-item">
      <input 
          type="submit"
          value="<spring:message code="toolbar.cut"/>"
          id="toolbar-cut-wai"
          name="prepareCut"/>
    </li>
    <li class="toolbar-item">
    <input 
        type="submit"
        value="<spring:message code="toolbar.paste"/>"
        id="toolbar-past-wai"
        name="past"/>
    <li class="toolbar-item">
    <input 
        type="submit"
        value="<spring:message code="toolbar.rename"/>"
        id="toolbar-rename-wai"
        name="rename"/>
  </li>
  <li class="toolbar-item">
  <input 
      type="submit"
      value="<spring:message code="toolbar.delete"/>"
      id="toolbar-delete-wai"
      name="delete"/>
</li>
<li class="toolbar-item">
<input type="submit" value="<spring:message code="toolbar.zip"/>" id="toolbar-zip-wai" name="zip"/>
</li>
<li class="toolbar-item">
<a  id="toolbar-create-wai" 
	href="<portlet:renderURL><portlet:param name="action" value="createFolderWai"/><portlet:param name="dir" value="${currentDir}"/><portlet:param name="sharedSessionId" value="${n}"/></portlet:renderURL>">
	<spring:message code="toolbar.create"/>
</a>
</li>
<li class="toolbar-item">
<a  id="toolbar-upload-wai" 
	href="<portlet:renderURL><portlet:param name="action" value="fileUploadWai"/><portlet:param name="dir" value="${currentDir}"/><portlet:param name="sharedSessionId" value="${n}"/></portlet:renderURL>">
	<spring:message code="toolbar.upload"/>
</a>
</li>
</ul>

</form:form>


</div>

</div>

</div>


