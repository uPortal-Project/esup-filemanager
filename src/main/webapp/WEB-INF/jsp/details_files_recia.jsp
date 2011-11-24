<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<h3 class="ui-widget-header ui-corner-all" ><spring:message code="details.header"/></h3>

<div class="details-spacer"></div>

<div class="details-attribute-header"><spring:message code="details.numselected" /> : </div>
<div class="details-attribute">${numselected}</div>

<div class="details-spacer"></div>
<div id="detail-zip-download"><spring:message code="toolbar.zip" /></div>

<c:if test="${not empty image_paths}">

	<div id="detail-view-images">
	   <spring:message code="details.images.title" />
	</div>
	
	<div id="diaporamaDivId" title='<spring:message code="details.images.title" />' >
	    <ul class="diaporama1">
	        <c:forEach var="path" items="${image_paths}">
	            <li><img
	                src="/esup-portlet-stockage/servlet-ajax/fetchImage?path=${path}&sharedSessionId=${sharedSessionId}"
	                alt="Image" title="${path}" />
	            </li>
	        </c:forEach>
	    </ul>
	</div>
	
	<div id ="ok_text" ><spring:message code="details.images.ok"/></div>
	<div id = "next_text" ><spring:message code="details.images.next"/></div>
	<div id ="prev_text" ><spring:message code="details.images.prev"/></div>

</c:if>




<script type="text/javascript">

( function($) {

$(document).ready(function () {
    $('#detail-zip-download').bind('click', function () {
        $.downloadZip();
    });

    init_diaporama();

    $('#detail-view-images').bind('click', function () {
        show_images();
    });

});

function init_diaporama() {
  var jDiaporamaElem =  $(".diaporama1");
  var jDiaporamaObj = jDiaporamaElem.jDiaporama({
    animationSpeed: "slow",
    paused: false,
    delay:5
  });

    jDiaporamaElem.data("obj", jDiaporamaObj);

}


function show_images() {

  var diapDiv = $("#detailArea #diaporamaDivId");

  var dialogButtons = {};

  dialogButtons[$("#prev_text").html()] = function() {
      $(".jDiaporama_controls .prev").click();
  };

  dialogButtons[$("#next_text").html()] = function() {
      $(".jDiaporama_controls .next").click();
  };

  dialogButtons[$("#ok_text").html()] = function () {
      //$(this).dialog("close");

      //Error in IE the 2nd time unless the dialog is destroyed completely
      $(this).dialog("destroy");

      //Put it back in details area

      var diapDivMoved = $("body #diaporamaDivId");

      diapDivMoved.remove();

      $("#detailArea #detail-view-images").after(diapDivMoved);
  };



    //$("body").css("cursor", "progress");
    diapDiv.dialog({
        modal: true,
        closeOnEscape: true,
        width:  640,
        height:  480,
        resizable: true,
        draggable: true,
        buttons: dialogButtons,
        open: function (event, ui) {
            console.log("Open dialog");
            //Workaround for IE as the height tends to be the height of the image regardless of the height passed in
            if (jQuery.browser.msie) {
              console.log("IE workaround");

              var dialog = diapDiv.closest(".ui-dialog");
              var dialog_content = diapDiv.closest(".ui-dialog-content");
              var dialog_titlebar = dialog.find("div.ui-dialog-titlebar");
              var dialog_buttonpane = dialog.find("div.ui-dialog-buttonpane");

              dialog.css("height", "600px");
              dialog_content.css("height", "490px");
              dialog_content.css("width", "624px");
              dialog_content.css("left", "8px");
              dialog_content.css("padding", "0");
              dialog_content.css("top", "4px");

              dialog_titlebar.css("left", "8px");
              dialog_titlebar.css("padding-left", "-4px");
              dialog_titlebar.css("padding-right", "-4px");
              dialog_titlebar.css("width", "100%");

              dialog_buttonpane.css("top", "4px");
              dialog_buttonpane.css("position", "relative");

              //console.log(dialog.html());
              //console.log(dialog_content.html());
            }

          }

    });
}

// Returns the cursor to the default pointer


function hide_image() {
    //$("body").css("cursor", "auto");
    $("#diaporamaDivId").dialog('close');
}

})(jQuery);

</script>