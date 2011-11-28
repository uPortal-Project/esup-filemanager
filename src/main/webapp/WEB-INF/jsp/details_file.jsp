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
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<h3 class="ui-widget-header ui-corner-all"><spring:message code="details.header"/></h3>

<div class="details-spacer"></div>

<div class="details-attribute-header"><spring:message code="details.title" /> : </div>
<div class="details-attribute"><img src="${file.icon}" alt="icon" /> ${file.title}</div>

<div class="details-attribute-header"><spring:message code="details.size" /> : </div>
<div class="details-attribute">
  ${file.formattedSize.size}
  <spring:message code="details.${file.formattedSize.unit}" />
</div>
<div class="details-attribute-header"><spring:message code="details.type" /> : </div>
<div class="details-attribute">${file.mimeType}</div>
<c:if test="${not empty file.lastModifiedTime}">
	<div class="details-attribute-header"><spring:message code="details.lastModifiedTime" /> : </div>
	<div class="details-attribute">${file.lastModifiedTime}</div>
</c:if>
<div class="details-spacer"></div>
<form:form method="post" id="detailsFileForm"
  action="/esup-portlet-stockage/servlet-ajax/downloadFile">

  <input name="sharedSessionId" type="hidden" value="${sharedSessionId}" />

  <input name="dir" type="hidden" value="${file.encPath}" />


  <div id="detail-download">
      <spring:message code="details.download" />
  </div>

</form:form>



<script type="text/javascript">

( function($) {
  $(document).ready(function(){
    $('#detail-download').bind('click', function() {
      $("#detailsFileForm").submit();
      return true;
  });
  });
})(jQuery);
</script>