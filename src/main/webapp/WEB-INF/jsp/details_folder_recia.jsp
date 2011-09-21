
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


<h3 class="ui-widget-header ui-corner-all" >
    <spring:message code="details.header" />
</h3>

<div class="details-spacer"></div>

<div class="details-attribute-header"><spring:message code="details.title" /> : </div>
<div class="details-attribute"><img src="${file.icon}" alt="icon" /> ${file.title}</div>

<div class="details-attribute-header"><spring:message code="details.folders" /> : </div>
<div class="details-attribute">${file.folderCount}</div>

<div class="details-attribute-header"><spring:message code="details.files" /> : </div>
<div class="details-attribute">${file.fileCount}</div>

<div class="details-attribute-header"><spring:message code="details.totalsize" /> : </div>
<div class="details-attribute">${file.formattedTotalSize.size} <spring:message code="details.${file.formattedTotalSize.unit}"/></div>

<div class="details-spacer"></div>