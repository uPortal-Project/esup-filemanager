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
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<h3 class="ui-widget-header ui-corner-all">
	<spring:message code="details.header" />
</h3>

<portlet:resourceURL id="fetchSound" var="fetchSoundURL">
	<portlet:param name="dir" value="${file.encPath}"/>
</portlet:resourceURL>

<portlet:resourceURL id="downloadFile" var="downloadFileURL" />


<audio controls style="display:none">
  <source src="${fetchSoundURL}" class="audioSource"></source>
</audio>

<div id="jquery_jplayer_1" class="jp-jplayer"></div>

<div id="jp_container_1" class="jp-audio">
	<div class="jp-type-single">
		<div id="jp_interface_1" class="jp-gui jp-interface">
			<ul class="jp-controls">
				<li><a href="javascript:;" class="jp-play" tabindex="1">play</a>
				</li>
				<li><a href="javascript:;" class="jp-pause" tabindex="1">pause</a>
				</li>
				<li><a href="javascript:;" class="jp-stop" tabindex="1">stop</a>
				</li>
				<li><a href="javascript:;" class="jp-mute" tabindex="1">mute</a>
				</li>
				<li><a href="javascript:;" class="jp-unmute" tabindex="1">unmute</a>
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
			<div class="jp-time-holder">
				<div class="jp-current-time"></div>
				<div class="jp-duration"></div>
			</div>
		</div>
		<div class="jp-no-solution">
			<span>Update Required</span> To play the media you will need to
			either update your browser to a recent version or update your <a
				href="http://get.adobe.com/flashplayer/" target="_blank">Flash
				plugin</a>.
		</div>
	</div>
</div>

<div class="details-spacer"></div>
<div class="details-attribute-header">
	<spring:message code="details.title" />
	:
</div>
<div class="details-attribute">
	<!--img src="${file.icon}" alt="icon" /--> ${file.title}
</div>

<div class="details-attribute-header">
	<spring:message code="details.size" />
	:
</div>

<div class="details-attribute">
	${file.formattedSize.size}
	<spring:message code="details.${file.formattedSize.unit}" />
</div>
<div class="details-attribute-header">
	<spring:message code="details.type" />
	:
</div>
<div class="details-attribute">${file.mimeType}</div>
<div class="details-attribute-header">
	<spring:message code="details.lastModifiedTime" />
	:
</div>
<div class="details-attribute">${file.lastModifiedTime}</div>

<div class="details-spacer"></div>




<form:form method="post" id="detailsFileForm">


	<input name="dir" type="hidden" value="${file.encPath}" />

	<div id="detail-download">
		<spring:message code="details.download" />
	</div>


</form:form>





<script type="text/javascript">

( function($) {

$(document).ready(function () {

  <c:if test="${not file.overSizeLimit}">

  console.log("Doc ready details sound");

  var audioSource_element = $("source.audioSource")[0];
  var soundUrl = audioSource_element.src;

  console.log("soundUrl: " + soundUrl);

   $("#jquery_jplayer_1").jPlayer({
          ready: function () {
              console.log("ready js player div");
            $(this).jPlayer("setMedia", {
              mp3: soundUrl
            });
          },
          swfPath: "/esup-filemanager/js",
          supplied: "mp3",
  		  wmode : "window"
        });

   </c:if>


    $('#detail-download').bind('click', function () {


        $("#detailsFileForm").attr("action", '${downloadFileURL}');

        $("#detailsFileForm").submit();
        return true;
    });


} );

})(jQuery);

</script>

