<%--

    Copyright (C) 2011 Esup Portail http://www.esup-portail.org
    Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
    @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
    @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
    @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
    @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
    @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>

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

<%@ taglib prefix='portlet' uri="http://java.sun.com/portlet"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<portlet:defineObjects />

<c:set var="n">
	<portlet:namespace />
</c:set>

<div class="portlet-title">
	<h2>
		<spring:message code="edit.title" />
	</h2>
</div>

<span><spring:message code="edit.legend"/></span>

<div class="portlet-section">

	<div class="portlet-section-body">
	  
	  <portlet:actionURL var="updatePreferencesUrl">
    	<portlet:param name="action" value="updatePreferences"/>
  	  </portlet:actionURL>
	
	  <form id="${n}updatePreferences" class="updatePreferences" action="${updatePreferencesUrl}" method="post">

			<c:if test="${not roViewMode}">
				<fieldset>
					<legend><spring:message code="edit.viewMode"/></legend>
					<ul>
						<li>
							<input type="radio" name="viewMode" value="standard" ${viewMode == 'standard'? 'checked="checked"' : ''}/><spring:message code="edit.viewMode.standard"/>
						</li>
						<li>
							<input type="radio" name="viewMode" value="wai" ${viewMode == 'wai'? 'checked="checked"' : ''}/><spring:message code="edit.viewMode.wai"/>
						</li>
						<li>
							<input type="radio" name="viewMode" value="mobile" ${viewMode == 'mobile'? 'checked="checked"' : ''}/><spring:message code="edit.viewMode.mobile"/>
						</li>									
					</ul>
				</fieldset>
			</c:if>
			<c:if test="${not roShowHiddenFiles}">
				<fieldset>
					<legend><spring:message code="edit.showHiddenFiles"/></legend>
					<input type="radio" name="showHiddenFiles" value="true" ${showHiddenFiles == 'true'? 'checked="checked"' : ''}/><spring:message code="edit.true"/> - 
					<input type="radio" name="showHiddenFiles" value="false" ${showHiddenFiles == 'false'? 'checked="checked"' : ''}/><spring:message code="edit.false"/> 
				</fieldset>
			</c:if>
			<c:if test="${not roUseCursorWaitDialog || not roUseDoubleClick}">
				<fieldset>
					<legend><spring:message code="edit.standardView"/></legend>
					<ul>
						<c:if test="${not roUseCursorWaitDialog}">
							<li>
								<spring:message code="edit.standardView.useCursorWaitDialog"/>
								<input type="radio" name="useCursorWaitDialog" value="true" ${useCursorWaitDialog == 'true'? 'checked="checked"' : ''}/><spring:message code="edit.true"/> - 
								<input type="radio" name="useCursorWaitDialog" value="false" ${useCursorWaitDialog == 'false'? 'checked="checked"' : ''}/><spring:message code="edit.false"/> 
							</li>
						</c:if>
						<c:if test="${not roUseDoubleClick}">
							<li>
								<spring:message code="edit.standardView.useDoubleClick"/>
								<input type="radio" name="useDoubleClick" value="true" ${useDoubleClick == 'true'? 'checked="checked"' : ''}/><spring:message code="edit.true"/> - 
								<input type="radio" name="useDoubleClick" value="false" ${useDoubleClick == 'false'? 'checked="checked"' : ''}/><spring:message code="edit.false"/> 
							</li>
						</c:if>									
					</ul>
				</fieldset>
			</c:if>
			<input type="submit" value="<spring:message code="edit.done"/>" class="portlet-form-button"/>
		</form>

	</div>

</div>
