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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<div class="esupstock">

	<div class="container">

		<div class="portlet-section">

			<div class="portlet-section-body">

				<div id="toolbar" class="fl-fix">
					<div class="separator">
						<span class="toolbar-item"> <a href="#"
							title="<spring:message code='toolbar.refresh.title'/>"
							class="disabled" id="toolbar-refresh" onclick="return false;">
								<spring:message code="toolbar.refresh" /> </a>
						</span>
						<span class="toolbar-item"> <a href="#" id="toolbar-thumbnail"
							class="disabled" onclick="return false;" style="display: none"
							title="<spring:message code='toolbar.thumbnail.title'/>"> <spring:message
									code="toolbar.thumbnail" /> </a>
						</span>
						<span class="toolbar-item">
							<a href="#" id="toolbar-list" class="disabled"
							onclick="return false;" style="display: none"
							title="<spring:message code='toolbar.list.title'/>"> <spring:message
									code="toolbar.list" /> </a>
						</span>
					</div>
					<div class="separator">
						<span class="toolbar-item" id="file-uploader"
							title="<spring:message code='toolbar.upload.title'/>"> </span> <span
							class="toolbar-item"> <a href="#" class="disabled"
							id="toolbar-new_folder"
							title="<spring:message code='toolbar.create.title'/>"> <spring:message
									code="toolbar.create" /> </a> </span> <span class="toolbar-item">
							<a href="#" class="disabled" id="toolbar-new_file"
							title="<spring:message code='toolbar.create.file.title'/>"> <spring:message
									code="toolbar.create.file" /> </a> </span>
					</div>
					<div class="separator">
						<span class="toolbar-item"> <a href="#" class="disabled"
							id="toolbar-download"
							title="<spring:message code='toolbar.download.title'/>"> <spring:message
									code="toolbar.download" /> </a> </span> <span class="toolbar-item">
							<a href="#" class="disabled" id="toolbar-zip"
							title="<spring:message code='toolbar.zip.title'/>"> <spring:message
									code="toolbar.zip" /> </a> </span>
					</div>
					<div>
						<span class="toolbar-item"> <a href="#" class="disabled"
							id="toolbar-copy" onclick="return false;"
							title="<spring:message code='toolbar.copy.title'/>"> <spring:message
									code="toolbar.copy" /> </a> </span> <span class="toolbar-item"> <a
							href="#" class="disabled" id="toolbar-cut"
							onclick="return false;"
							title="<spring:message code='toolbar.cut.title'/>"> <spring:message
									code="toolbar.cut" /> </a> </span> <span class="toolbar-item"> <a
							href="#" class="disabled" id="toolbar-paste"
							onclick="return false;"
							title="<spring:message code='toolbar.paste.title'/>"> <spring:message
									code="toolbar.paste" /> </a> </span> <span class="toolbar-item"> <a
							href="#" class="disabled" id="toolbar-rename"
							onclick="return false;"
							title="<spring:message code='toolbar.rename.title'/>"> <spring:message
									code="toolbar.rename" /> </a> </span> <span class="toolbar-item">
							<a href="#" class="disabled" id="toolbar-delete"
							onclick="return false;"
							title="<spring:message code='toolbar.delete.title'/>"> <spring:message
									code="toolbar.delete" /> </a> </span>
					</div>
					
					<%--
					<div class="toolbar-logout-div"> 
						<span class="toolbar-item"> 
							<a id="toolbar-logout" href="/esup-filemanager/servlet-ajax/logout">
								<spring:message	code="toolbar.logout" />
							</a>
						</span>
					</div>
					 --%>
				</div>

				<div id="info-toolbar" class="fl-col-flex1">
					<span>...</span>
				</div>
				<!--  GIP RECIA add id="resizable" and the css class ui-widget-content-->

				<div class="fl-col-flex2">

					<div id="leftArea" class="fl-col fl-force-left">
					     <h3 class="ui-widget-header ui-corner-all arborescentAreaTitle">
						  	<spring:message code="treearea.header" />
						</h3>
						<div id="arborescentArea">
							<div id="fileTree">
								<spring:message code="error.javascriptengine" />
							</div>
						</div>
						<div id="detailArea"></div>
					</div>

					<div id="browserArea" class="ui-widget-content fl-force-right">
						<div id="browserMain"></div>
					</div>

				</div>


			</div>

		</div>

		<!--  GIP RECIA : The context menu of the browser area ; this isn't the toolbar -->
		<ul id="myMenu" class="contextMenu">
			<li class="download"><a href="#download"> <spring:message
						code="browserarea.menu.download" /> </a></li>
			<li class="zip"><a href="#zip"> <spring:message
						code="browserarea.menu.zip" /> </a></li>
			<li class="copy separator"><a href="#copy"> <spring:message
						code="browserarea.menu.copy" /> </a></li>
			<li class="cut"><a href="#cut"> <spring:message
						code="browserarea.menu.cut" /> </a></li>
			<li class="paste"><a href="#paste"> <spring:message
						code="browserarea.menu.past" /> </a></li>
			<li class="rename "><a href="#rename"> <spring:message
						code="browserarea.menu.rename" /> </a></li>
			<li class="delete"><a href="#delete"> <spring:message
						code="browserarea.menu.delete" /> </a></li>
		</ul>

		<div id="waitingDialog"
			title="<spring:message code="ajax.overlay.title"/>">
			<spring:message code="ajax.overlay.body" />
		</div>

		<div id="errorDialog"
			title="<spring:message code="ajax.error.title"/>">
			...
		</div>

	</div>

	<div id="delete-confirm-ok">
		<spring:message code='delete.confirm.ok' />
	</div>
	<div id="delete-confirm-cancel">
		<spring:message code='delete.confirm.cancel' />
	</div>

	<div id="delete-confirm"
		title="<spring:message code='delete.confirm.title'/>">
		<p>
			<span class="ui-icon ui-icon-alert"></span>
			<spring:message code="delete.confirm.message" />
		</p>
	</div>

</div>
