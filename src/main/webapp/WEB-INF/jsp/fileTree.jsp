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

<div id="thumbnail_mode" style="display:none">false</div>

<div id="jqueryFileTree" >
  <form:form method="post" id="filesForm">

    <table>
      <thead>
        <tr>
          <th><spring:message code="browserArea.header.filename" /></th>
          <th><spring:message code="browserArea.header.size" /></th>
          <th><spring:message code="browserArea.header.type" /></th>
          <th><spring:message code="browserArea.header.modified" /></th>
        </tr>
      </thead>
      <tbody id="jqueryFileTreeBody">
        <c:forEach var="file" items="${files}">

          <c:choose>
            <c:when test="${file.type == 'folder'}">
              <tr class="selectable ${file.readable ? 'esup-stock-read' : ''} ${file.writeable ? 'esup-stock-write' : ''} ${file.hidden ? 'esup-stock-hidden' : ''}">
                <td class="checkbox">
                  <div class="readable">${file.readable}</div>
                  <div class="writeable">${file.writeable}</div>
                  <div class="draggable droppable">
                    <form:checkbox path="dirs" cssClass="browsercheck" value="${file.encPath}" />

                    <a class="fileTreeRef" href="" title="${file.title}" rel="${file.encPath}"
                      onclick="return false;">
                      <img src="${file.icon}" alt="icon" />
                      ${file.title}
                    </a>
                    <span class="esupHide renameSpan">
                      <input class="renameInput" type="text" value="${file.title}"
                        onKeyPress="return disableEnterKey(event)" />
                      <a class="renameSubmit" href="#">
                        <spring:message code="toolbar.rename" />
                      </a>
                    </span>
                  </div>
                </td>
                <td>-</td>
                <td><spring:message code="browserarea.directory" />
                </td>
                <td>${file.lastModifiedTime}</td>
              </tr>
            </c:when>
            <c:when test="${file.type == 'drive'}">
              <tr>
                <td><div class="droppable">
                    <img src="${file.icon}" alt="icon" />
                    <a class="fileCatRef" href="" title="${file.title}" rel="${file.encPath}"
                      onclick="return false;"> ${file.title} </a>
                  </div></td>
                <td />
                <td />
                <td />
              </tr>
            </c:when>
            <c:when test="${file.type == 'category'}">
              <tr>
                <td><div>
                    <img src="${file.icon}" alt="icon" />
                    <a class="fileCatRef" href="" title="${file.title}" rel="${file.encPath}"
                      onclick="return false;"> ${file.title} </a>
                  </div></td>
                <td />
                <td />
                <td />
              </tr>
            </c:when>
            <c:otherwise>
              <tr class="selectable">
                <td class="checkbox">
                  <div class="draggable">
                    <div class="readable">${file.readable}</div>
                    <div class="writeable">${file.writeable}</div>
                    <form:checkbox path="dirs" cssClass="browsercheck" value="${file.encPath}" />

                    <a class="file" href="" title="${file.title}" rel="${file.encPath}"
                      onclick="return false;">
                      <img src="${file.icon}" alt="icon" />
                      ${file.title}
                    </a>
                    <span class="esupHide renameSpan">
                      <input class="renameInput" type="text" value="${file.title}"
                        onKeyPress="return disableEnterKey(event)" />
                      <a class="renameSubmit" href="#">
                        <spring:message code="toolbar.rename" />
                      </a>
                    </span>
                  </div>
                </td>
                <td>${file.formattedSize.size} <spring:message
                    code="details.${file.formattedSize.unit}" /></td>
                <td><spring:message code="browserarea.file" /></td>
                <td>${file.lastModifiedTime}</td>
              </tr>
            </c:otherwise>
          </c:choose>

        </c:forEach>

      <tr>
        <td class="browserlist esupHide" id="newDir">
          <input type="hidden" id="folderOrFileChoice" value="folder" />
          <input class="folder" id="newFileOrFolderInput" type="text"
            onKeyPress="return disableEnterKey(event)" />
          <a id="newFileOrFolderSubmit" href="#"> OK </a>  
        </td>
      </tr>

      </tbody>
    </table>
    
  </form:form>
</div>

