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

<portlet:resourceURL id="fetchImage" var="fetchImageURL">
	<portlet:param name="dir" value="${file.encPath}"/>
</portlet:resourceURL>

<portlet:resourceURL id="downloadFile" var="downloadFileURL" />
				
  <h3 class="ui-widget-header ui-corner-all">
    <spring:message code="details.header" />
  </h3>


  <img
    src="${fetchImageURL}"
    class="detailsImage"
    alt="image" />


<div class="details-spacer"></div>
<div class="details-attribute-header"><spring:message code="details.title" /> : </div>
<div class="details-attribute"><!--img src="${file.icon}" alt="icon" /--> ${file.title}</div>

<div class="details-attribute-header"><spring:message code="details.imagesize" /> : </div>
<div class="details-attribute"><div id="image_width"></div>px X <div id="image_height"></div>px</div>

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

    <input name="dir" type="hidden" value="${file.encPath}" />


    <div id="detail-download" >

        <spring:message code="details.download" />

    </div>

    <div id="detail-view-image" >
     <spring:message code="details.viewimage" />
    </div>
  </form:form>


<div class="details-spacer"></div>

<script type="text/javascript">

( function($) {

	function show_image(image_width, image_height) {

		  //Manually cleanup the dialogs sometimes there are some dialogs hanging around
		  $("#image_original_size").remove();
		  $("body .image-dialog").remove();


		  var img_element = $("img.detailsImage")[0];

		  var divDialog = $("<div>");
		  divDialog.attr("id", "image_original_size");
		  divDialog.attr("title", "<spring:message code='details.image.title'/>");

		  var imgDialog = $("<img>");
		  imgDialog.attr("src", img_element.src);
		  imgDialog.attr("id", "image_original_size_img");

		  divDialog.append(imgDialog);

		  var dialogButtons = {};

		  dialogButtons[ "<spring:message code='details.images.ok'/>" ] = function () {
		      $(this).dialog("close");
		  };

		  var maxWidth = 0.8 * $(window).width();
		  var maxHeight = 0.8 * $(window).height();

		  var ratio = 1.0;

		  if (maxWidth < image_width) {
		    ratio = image_width / maxWidth;
		  }

		  if (maxHeight < image_height) {
		    ratio = Math.max(ratio, image_height / maxHeight);
		  }

		  image_width = image_width / ratio;
		  image_height = image_height / ratio;

		  imgDialog.width(image_width);
		  imgDialog.height(image_height);

		  //$("body").css("cursor", "progress");
		  divDialog.dialog({
		    modal: true,
		    closeOnEscape: true,
		    width: 30 + image_width,
		    height: 110 + image_height,
		    resizable: true,
		    dialogClass: 'image-dialog',
		    draggable: true,
		    autoOpen: false,
		    buttons: dialogButtons
		  });

		  divDialog.dialog("open");

		}

		// Returns the cursor to the default pointer

		$(document).ready(function () {

		  console.log("Details image page ready");
		  var img_element = $("img.detailsImage")[0];

		  console.log("details_image.jsp : img_element.src : " + img_element.src);

		  var image_width;
		  var image_height;

		  $("<img/>") // Make in memory copy of image to avoid css issues
		  .attr("src", img_element.src)
		  //Run the load event handler only once
		  .one('load', function () {
		    console.log("Image loaded");
		    image_width = this.width;
		    image_height = this.height;

		    //Scale the minature, also handle proportionately wide or tall images
		    if (image_width < 200 && image_height < 200) {
		      //small image                
		      //set miniature to be the actual dimensions as both are < 200
		      $(".detailsImage").css("width", image_width);
		      $(".detailsImage").css("height", image_height);
		    } else if (image_height > 200 && image_height >= image_width) {
		      //taller images / rectangular, limit height and scale width (width will always be less than 200)
		      $(".detailsImage").css("width", "auto");
		      $(".detailsImage").css("height", "200px");
		    } else if (image_width > 200 && image_width > image_height) {
		      //Similar case to tall images
		      $(".detailsImage").css("width", "200px");
		      $(".detailsImage").css("height", "auto");
		    }

		    $("#image_width").html('' + image_width);
		    $("#image_height").html('' + image_height);
		    $("#detail-view-image").show();

		  }).each(function () {
		    //In case the image is already cached, we need to make sure that load was called
		    if (this.complete) $(this).load();
		  });

		  $('#detail-download').bind('click', function () {
		    $("#detailsFileForm").attr("action", '${downloadFileURL}');
		    $("#detailsFileForm").submit();
		    return true;
		  });

		  var imageDetailLink = $('#detail-view-image');
		  imageDetailLink.unbind('click');

		  imageDetailLink.bind('click', function () {
		    console.log("Image detail click");
		    show_image(image_width, image_height);
		  });

		});

})(jQuery);

</script>
