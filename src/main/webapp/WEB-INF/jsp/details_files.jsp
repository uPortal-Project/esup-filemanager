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
	</div>

    <div id="diaporamaSource" style="display:none" >
	    <ul class="diaporama1">
	        <c:forEach var="path" items="${image_paths}">
	            <li><img
	                src="/esup-filemanager/servlet-ajax/fetchImage?path=${path}&sharedSessionId=${sharedSessionId}"
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
    
    $('#detail-view-images').bind('click', function () {
    	var diapWidth = 0.8 * $(window).width();
        var diapHeight = 0.8 * $(window).height();

        console.log("Diap width/height " + diapWidth + ", " + diapHeight);

        //Adjustments done by experimentation (where scrollbars are not visible)
        var imgWidth = diapWidth - 40;
        var imgHeight = diapHeight - 137;

        console.log("Image max w/h " + imgWidth + ", " + imgHeight);

        setoptions_diaporama(imgWidth, imgHeight);

        show_images(diapWidth, diapHeight);
      });

	});

	function setoptions_diaporama(imgWidth, imgHeight) {
	  console.log("setoptions_diaporama Img max width " + imgWidth);

	  $("#diaporamaDivId").html( $("#diaporamaSource").html() );
	  
	  $("#diaporamaDivId .diaporama1").jDiaporama( {
	  	animationSpeed: "slow",
	    paused: false,
	    delay: 5,
	    maxImageWidth: imgWidth,
	    maxImageHeight: imgHeight
	  });
	}


	function show_images(diapWidth, diapHeight) {

	  var diapDiv = $("#diaporamaDivId");

	  //Create hash of dialog buttons, the key being their localized name and the value being the function executed 
	  var dialogButtons = {};

	  dialogButtons[$("#prev_text").html()] = function () {
	    $(".jDiaporama_controls .prev").click();
	  };

	  dialogButtons[$("#next_text").html()] = function () {
	    $(".jDiaporama_controls .next").click();
	  };

	  dialogButtons[$("#ok_text").html()] = function () {
	    $(this).dialog("close");
	  };



	  //$("body").css("cursor", "progress");
	  diapDiv.dialog({
	    modal: true,
	    closeOnEscape: true,
	    width: diapWidth,
	    height: diapHeight,
	    resizable: true,
	    draggable: true,
	    buttons: dialogButtons,
	    close: function (event, ui) {
	      console.log("View images dialog close");
	      //Error in IE the 2nd time unless the dialog is destroyed completely
	      $(this).dialog("destroy");

	      //Put it back in details area

	      var diapDivMoved = $("body #diaporamaDivId");
	      diapDivMoved.remove();      
	      $("#detailArea #detail-view-images").after(diapDivMoved);
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