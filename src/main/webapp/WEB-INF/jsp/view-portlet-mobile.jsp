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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<portlet:defineObjects />

<c:set var="n">
  <portlet:namespace />
</c:set>

    <link 
        rel="stylesheet"
        href="/esup-filemanager/css/esup-stock-mobile.css"
        type="text/css"
        media="screen, projection"/>



  <div style="fl-theme-iphone">


    <div class="esupstock">

      <div class="breadcrumbs">
        <c:forEach var="parent" items="${parentsEncPathes}" varStatus="item">
          <c:choose>
            <c:when test="${item.last}">
              <img src="${resource.icon}" alt="icon" />
              <span>
                ${resource.title}
              </span>
            </c:when>
            <c:otherwise>
              <a 
                  href="<portlet:renderURL> <portlet:param name="action" value="browseMobile"/> <portlet:param name="dir" value="${parent.key}"/></portlet:renderURL>">



                <img src="${parent.value[1]}" alt="icon" />
                ${parent.value[0]}
              </a>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </div>

      <ul id="jqueryFileTree" style="">

        <c:forEach var="file" items="${files}">
          <li class="browserlist fl-container">
          <c:choose>
            <c:when test="${'file' == file.type}">
              <img src="${file.icon}" alt="icon" />
              <portlet:resourceURL id="downloadFile" var="downloadFileURL">
				 <portlet:param name="dir" value="${file.encPath}"/>
			  </portlet:resourceURL>
              <a class="file" href="${downloadFileURL}">
                ${file.title}
              </a>
            </c:when>
            <c:otherwise>
              <img src="${file.icon}" alt="icon" />
              <a 
                  class="fileTreeRef"
                  href="<portlet:renderURL><portlet:param name="action" value="browseMobile"/><portlet:param name="dir" value="${file.encPath}"/></portlet:renderURL>">



                ${file.title}
              </a>
            </c:otherwise>
          </c:choose>
        </li>
      </c:forEach>

    </ul>

  </div>

</div>
