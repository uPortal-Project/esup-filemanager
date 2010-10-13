<%--

    Copyright (C) 2010 Esup Portail http://www.esup-portail.org
    Copyright (C) 2010 UNR RUNN http://www.unr-runn.fr
    @Author (C) 2010 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2010 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<div class="breadcrumbs">
<c:forEach var="parent" items="${resource.parentsPathes}" varStatus="item">
	<c:choose>
		<c:when test="${item.last}">
			<img src="${resource.icon}" alt="icon" />
			<span id="bigdirectory" rel="${resource.path}">${resource.title}</span>
		</c:when>
		<c:otherwise>
			<a class="fileTreeRefCrumbs" href="#" rel="${parent.key}">
				<img src="${parent.value[1]}" alt="icon" />
				${parent.value[0]}
			</a>
		</c:otherwise>
	</c:choose>	
</c:forEach>
</div>

<c:if test="${not empty files[0] && files[0].type != 'category' && files[0].type != 'drive'}">
  <input type="checkbox" id="toggler-checkbox"/>
</c:if>

<ul id="jqueryFileTree">

	<form:form method="post" id="filesForm">
		<c:forEach var="file" items="${files}">
			<li class="browserlist"> 
			<c:choose>
				<c:when test="${file.type == 'folder'}">
					<form:checkbox path="dirs" cssClass="browsercheck" value="${file.path}" />
					<img src="${file.icon}" alt="icon" />
					<a class="fileTreeRef" href="#" rel="${file.path}">${file.title}</a></li>
				</c:when>
				<c:when test="${file.type == 'drive' || file.type == 'category'}">
					<img src="${file.icon}" alt="icon" />
					<a class="fileTreeRef" href="#" rel="${file.path}">${file.title}</a></li>
				</c:when>
				<c:otherwise>
					<form:checkbox path="dirs" cssClass="browsercheck" value="${file.path}" />
					<img src="${file.icon}" alt="icon" />
					<a class="file"
						href="/esup-portlet-stockage/servlet-ajax/downloadFile?dir=${file.path}"
						rel="${file.path}">${file.title}</a>
				</c:otherwise>
			</c:choose>
			<span class="esupHide renameSpan">
				<input class="renameInput" rel="${file.path}" type="text" value="${file.title}" onKeyPress="return disableEnterKey(event)"/>
				<a class="renameSubmit" rel="${file.path}" href="#">Rename</a>
			</span>
			</li>
		</c:forEach>
		<li class="browserlist esupHide" id="newDir">
			<input class="folder" id="newDirInput" type="text" onKeyPress="return disableEnterKey(event)"/>
			<a id="newDirSubmit" href="#">OK</a>
		</li>
	</form:form>

</ul>

<script type="text/javascript">

function disableEnterKey(e)
{
     var key;     
     if(window.event)
          key = window.event.keyCode; //IE
     else
          key = e.which; //firefox     
     return (key != 13);
}



(function ($) {
	$('.fileTreeRef').bind('click', function() {
		id = $(this).attr('rel');
		getFile($("#bigdirectory").attr('rel'), id);
	});
	
	$('.fileTreeRefCrumbs').bind('click', function() {
		id = $(this).attr('rel');
		getFile(null, id);
	});

	$('#newDirSubmit').bind('click', function() {
		newDir($("#bigdirectory").attr('rel'), $("#newDirInput").val());
	});

	$('#newDirInput').keyup(function(e) {
	      if(e.keyCode == 13) {
	    	  newDir($("#bigdirectory").attr('rel'), $("#newDirInput").val());
	      }
	 });

	$('.renameSubmit').bind('click', function() {
		rename($("#bigdirectory").attr('rel'), $(this).attr('rel'), $(this).prev().val());
	});

	$('.renameInput').keyup(function(e) {
	      if(e.keyCode == 13) {
			rename($("#bigdirectory").attr('rel'), $(this).attr('rel'), $(this).val());
	      }
	});

	$('#toggler-checkbox').change(function() {
	    var mode = this.checked;
	    $("form#filesForm :checkbox").each(function(){
	        this.checked = mode;
	    }); 
	    return false;
	});
	
})(jQuery);	

</script>
