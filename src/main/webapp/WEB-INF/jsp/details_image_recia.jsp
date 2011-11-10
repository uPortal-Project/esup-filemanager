
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


  <h3 class="ui-widget-header ui-corner-all">
    <spring:message code="details.header" />
  </h3>


  <img
    src="/esup-portlet-stockage/servlet-ajax/fetchImage?path=${urlEncPath}&sharedSessionId=${sharedSessionId}"
    class="detailsImage"
    alt="image" />


<div class="details-spacer"></div>
<div class="details-attribute-header"><spring:message code="details.title" /> : </div>
<div class="details-attribute"><img src="${file.icon}" alt="icon" /> ${file.title}</div>

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

    <input name="sharedSessionId" type="hidden" value="${sharedSessionId}" />

    <input name="dir" type="hidden" value="${path}" />


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


  //$("body").css("cursor", "progress");
  divDialog.dialog({
    modal: true,
    closeOnEscape: true,
    width: 30 + Math.min(900, image_width),
    height: 110 + Math.min(600, image_height),
    resizable: true,
    dialogClass: 'image-dialog',
    draggable: true,
    autoOpen: false,
    buttons: dialogButtons,
    close: function (event, ui) {
      console.log("Destroying dialog");
      //$(this).dialog("destroy");
    },
    open: function (event, ui) {
      console.log("Open dialog");
      //Workaround for IE as the height tends to be the height of the image regardless of the height passed in
      if (jQuery.browser.msie) {
        console.log("IE workaround");

        var dialog = $('#image_original_size_img').closest(".ui-dialog");
        var dialog_content = $('#image_original_size_img').closest(".ui-dialog-content");

        dialog.css("height", "600px");
        dialog_content.css("height", "500px");
        dialog_content.css("padding", "0");
        dialog_content.css("top", "4px");

        $('#image_original_size').css("width", "100%");
        $('#image_original_size').css("height", "500");

        $('div.ui-widget-overlay').css("height", "auto");
      }

    }
  });

  divDialog.dialog("open");

}

// Returns the cursor to the default pointer

$(document).ready(function () {

  console.log("Details image page ready");
  var img_element = $("img.detailsImage")[0];

  console.log("details_image_recia.jsp : img_element.src : " + img_element.src);

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
    $("#detailsFileForm").attr("action", '/esup-portlet-stockage/servlet-ajax/downloadFile');
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