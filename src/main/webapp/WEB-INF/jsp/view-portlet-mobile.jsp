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
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<portlet:defineObjects />

<c:set var="n">
  <portlet:namespace />
</c:set>

    <link 
        rel="stylesheet"
        href="/esup-filemanager/css/esup-stock-mobile.css"
        type="text/css"
        media="screen, projection"/>


    <div id="${n}EsupFilemanager" class="ui-content esupstock" role="main" data-role="content" data-theme="c">
      <div data-role="controlgroup" data-mini="true" data-type="horizontal" class="breadcrumbs">
        <c:forEach var="parent" items="${parentsEncPathes}" varStatus="item">
          <%-- last item IS current resource --%>
          <c:choose>
            <c:when test="${item.last}">
              <span data-role="button">
                <img src="${resource.icon}" alt="icon" />
		<span>${resource.title}</span>
             </c:when>
             <c:otherwise>
               <portlet:renderURL var="buttonUrl" escapeXml="true"> 
		 <portlet:param name="action" value="browseMobile"/>
		 <portlet:param name="dir" value="${parent.key}"/>
               </portlet:renderURL>
               <a data-role="button" href="${buttonUrl}">
		 <img src="${parent.value[1]}" alt="icon" />
		 ${parent.value[0]}
		 <c:set var="parentItemPath" value="${parent}"/>
               </a>
             </c:otherwise>
	  </c:choose>
        </c:forEach>
      </div>


      <div class="ui-body">
      	<ul id="jqueryFileTree" data-role="listview" data-inset="true">
	  <%-- list of files --%>
          <c:forEach var="file" items="${files}">
            <c:choose>
              <c:when test="${'file' == file.type}">
	        <portlet:resourceURL id="downloadFile" var="downloadFileURL" escapeXml="true">
		  <portlet:param name="dir" value="${file.encPath}"/>
		</portlet:resourceURL>
		<li data-icon="false">
		  <a href="${downloadFileURL}">
		    <img src="${file.icon}" alt="icon" class="ui-li-icon"/>
		    ${file.title}
		  </a>
		  <p class="listAttributes">
		    <span class="listAttribute">
		      <span class="attrLabel"><spring:message code="browserArea.header.size" htmlEscape="true"/> : </span>
		      <span class="attrValue">${file.formattedSize.size}&#160;<spring:message code="details.${file.formattedSize.unit}"/></span>
	            </span>
		    <span class="listAttribute">
		      <span class="attrLabel"><spring:message code="browserArea.header.modified" htmlEscape="true"/> : </span>
		      <span class="attrValue">${file.lastModifiedTime}</span>
	            </span>
	          </p>
	        </li>
              </c:when>
            <c:otherwise>
	      <portlet:renderURL var="folderUrl" escapeXml="true">
	        <portlet:param name="action" value="browseMobile"/>
	        <portlet:param name="dir" value="${file.encPath}"/>
	      </portlet:renderURL>
	      <li>
		<a href="${folderUrl}">
		  <img src="${file.icon}" alt="icon" class="ui-li-icon"/>
		  ${file.title}
		</a>
              </li>
            </c:otherwise>
            </c:choose>
	  </c:forEach>
	</ul>
      </div>
    </div>

