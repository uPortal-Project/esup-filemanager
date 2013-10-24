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
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<h3 class="ui-widget-header ui-corner-all" >
    <spring:message code="details.header" />
</h3>

<dl class="details">

<dt class="details-attribute-header"><spring:message code="details.title" /> : </dt>
<dd class="details-attribute"><img src="${file.icon}" alt="icon" /> ${file.title}</dd>

<dt class="details-attribute-header"><spring:message code="details.folders" /> : </dt>
<dd class="details-attribute">${file.folderCount}</dd>

<dt class="details-attribute-header"><spring:message code="details.files" /> : </dt>
<dd class="details-attribute">${file.fileCount}</dd>

<dt class="details-attribute-header"><spring:message code="details.totalsize" /> : </dt>
<dd class="details-attribute">${file.formattedTotalSize.size} <spring:message code="details.${file.formattedTotalSize.unit}"/></dd>

<c:if test="${not empty quota}">
	<dt class="details-attribute-header"><spring:message code="details.quota" /> : </dt>
	<dd class="details-attribute"> ${quota.usage}% (${quota.usedSize} ${quota.usedUnit} / ${quota.maxSize} ${quota.maxUnit} )</dd>
</c:if>

</dl>