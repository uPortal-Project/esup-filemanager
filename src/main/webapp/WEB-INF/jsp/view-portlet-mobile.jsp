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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix='portlet' uri="http://java.sun.com/portlet" %>

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


    <div class="esupstock">

      <div class="breadcrumbs">
        <c:forEach var="parent" items="${resource.parentsPathes}" varStatus="item">
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
              <a class="file" href="/esup-portlet-stockage/servlet-ajax/downloadFile?dir=${file.path}">
                ${file.title}
              </a>
            </c:when>
            <c:otherwise>
              <img src="${file.icon}" alt="icon" />
              <a 
                  class="fileTreeRef"
                  href="<portlet:renderURL><portlet:param name="action" value="browseMobile"/><portlet:param name="dir" value="${file.path}"/></portlet:renderURL>">



                ${file.title}
              </a>
            </c:otherwise>
          </c:choose>
        </li>
      </c:forEach>

    </ul>

  </div>

</body>

</html>
