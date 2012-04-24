<%--

    Copyright (C) 2012 Esup Portail http://www.esup-portail.org
    Copyright (C) 2012 UNR RUNN http://www.unr-runn.fr
    Copyright (C) 2012 RECIA http://www.recia.fr
    @Author (C) 2012 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2012 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
    @Contributor (C) 2012 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
    @Contributor (C) 2012 Julien Gribonvald <Julien.Gribonvald@recia.fr>
    @Contributor (C) 2012 David Clarke <david.clarke@anu.edu.au>
    @Contributor (C) 2012 BULL http://www.bull.fr
    @Contributor (C) 2012 Pierre Bouvret <pierre.bouvret@u-bordeaux4.fr>
    @Contributor (C) 2012 Franck Bordinat <franck.bordinat@univ-jfc.fr>

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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<div class="breadcrumbs">
  <c:forEach var="parent" items="${parentsEncPathes}" varStatus="item">
    <c:choose>
      <c:when test="${item.last}">
        <img src="${resource.icon}" alt="icon" />
        <span id="bigdirectory" rel="${resource.encPath}"> ${resource.title} </span>
      </c:when>
      <c:otherwise>
        <a class="fileTreeRefCrumbs" href="#" rel="${parent.key}">
          <img src="${parent.value[1]}" alt="icon" />
          ${parent.value[0]}
        </a>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</div>

<div id="thumbnail_mode" style="display:none">true</div>

<div id="jqueryFileTree" class="selectable">
  <form:form method="post" id="filesForm">

    <input name="sharedSessionId" type="hidden" value="${sharedSessionId}" />

    <c:forEach var="file" items="${files}">

      <div class="thumbnail ${(file.type == 'folder') ? 'droppable' : ''}">

        <c:if test="${file.type == 'folder' || file.type == 'file'}">
          <div class="readable">${file.readable}</div>
          <div class="writeable">${file.writeable}</div>
        </c:if>

        <div
          class="${ (file.type == 'folder' || file.type == 'file') ? 'draggable' : ''} inner_thumbnail">

          <c:if test="${file.type == 'folder' || file.type == 'file'}">
            <form:checkbox path="dirs" cssClass="browsercheck" value="${file.encPath}" />
          </c:if>

          <a
            class="${ (file.type == 'drive' || file.type == 'category') ? 'fileCatRef' :
               (file.type == 'folder') ? 'fileTreeRef' : 'file'}"
            href="" rel="${file.encPath}" title="${file.title}" onclick="return false;">
            <img src="${file.icon}" alt="icon" />
            <span
              class="thumbnailLinkText ${ (file.type == 'folder' || file.type == 'file') ? 'selectable' : ''}">${file.truncatedTitle}</span>

          </a>


          <c:if test="${file.type == 'folder' || file.type == 'file'}">
            <span class="esupHide renameSpan">
              <input class="renameInput" type="text" value="${file.title}"
                onKeyPress="return disableEnterKey(event)" />
              <a class="renameSubmit" href="#">
                <!--<spring:message code="toolbar.rename" />-->
              </a>
            </span>
          </c:if>

        </div>
      </div>


    </c:forEach>

    <div class="thumbnail browserlist esupHide" id="newDir">
      <div class="inner_thumbnail">
      <input type="hidden" id="folderOrFileChoice" value="folder" />
      <input class="folder" id="newFileOrFolderInput" type="text"
        onKeyPress="return disableEnterKey(event)" />
      <a id="newFileOrFolderSubmit" href="#"> OK </a>
      </div>
    </div>
  </form:form>
</div>

