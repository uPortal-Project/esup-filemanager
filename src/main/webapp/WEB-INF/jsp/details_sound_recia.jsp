
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

  <h3 class="ui-widget-header ui-corner-all" ><spring:message code="details.header"/></h3>

  <div id="jquery_jplayer_1" class="jp-jplayer"></div>
  <div class="jp-audio">
    <div class="jp-type-single">
      <div id="jp_interface_1" class="jp-interface">
        <ul class="jp-controls">
          <li>
            <a href="#" class="jp-play" tabindex="1">play</a>
          </li>
          <li>
            <a href="#" class="jp-pause" tabindex="1">pause</a>
          </li>
          <li>
            <a href="#" class="jp-stop" tabindex="1">stop</a>
          </li>
          <li>
            <a href="#" class="jp-mute" tabindex="1">mute</a>
          </li>
          <li>
            <a href="#" class="jp-unmute" tabindex="1">unmute</a>
          </li>
        </ul>
        <div class="jp-progress">
          <div class="jp-seek-bar">
            <div class="jp-play-bar"></div>
          </div>
        </div>
        <div class="jp-volume-bar">
          <div class="jp-volume-bar-value"></div>
        </div>
        <div class="jp-current-time"></div>
        <div class="jp-duration"></div>
      </div>
      <div id="jp_playlist_1" class="jp-playlist">
        <ul>
          <li>${file.title}</li>
        </ul>
      </div>
    </div>
  </div>
  
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

   


    <form:form method="post" id="detailsFileForm">

      <input name="sharedSessionId" type="hidden" />

      <input name="dir" type="hidden" value="${path}" />


      <div id="detail-download">
          <spring:message code="details.download" />        
      </div>


    </form:form>
  



<script type="text/javascript">

    
$(document).ready(function () {

	<% if(! ((org.esupportail.portlet.stockage.beans.JsTreeFile) request.getAttribute("file")).isOverSizeLimit() ) { %>
	  
	console.log("Doc ready details sound");
    
	 $("#jquery_jplayer_1").jPlayer({
		 solution:"flash" , //, html",
	        ready: function () {
              console.log("ready js player div");
	          $(this).jPlayer("setMedia", {
	            mp3: "/esup-portlet-stockage/servlet-ajax/fetchSound?path=${path}&sharedSessionId=${sharedSessionId}"
	          });
	        },
	        swfPath: "..${isPortlet ? '/esup-portlet-stockage' : ''}/js",
	        supplied: "mp3"
	      });



	 <% } %>
    

    $('#detail-download').bind('click', function () {


        $("#detailsFileForm").attr("action", '/esup-portlet-stockage/servlet-ajax/downloadFile');

        //Set the sharedSessionId in the hiddeninputfield
        $("#detailsFileForm.sharedSessionId").val(sharedSessionId);
        $("#detailsFileForm").submit();
        return true;
    });

    
} );

  
  

        
        
        </script>