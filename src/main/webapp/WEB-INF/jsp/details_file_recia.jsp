
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


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
<div class="details-attribute-header"><spring:message code="details.lastModifiedTime" /> : </div>
<div class="details-attribute">${file.lastModifiedTime}</div>

<div class="details-spacer"></div>
<form:form method="post" id="detailsFileForm"
  action="/esup-portlet-stockage/servlet-ajax/downloadFile">

  <input name="sharedSessionId" type="hidden" value="${sharedSessionId}" />

  <input name="dir" type="hidden" value="${path}" />


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