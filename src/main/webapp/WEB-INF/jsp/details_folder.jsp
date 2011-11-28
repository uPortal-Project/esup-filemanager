<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


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

</dl>