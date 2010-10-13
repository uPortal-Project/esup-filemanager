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

<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>


<div class="esupstock">

<div class="container">

<div class="portlet-section">

<div class="portlet-section-body">&nbsp;

<div id="toolbar" class="span-24 last">
 <div class="span-12 colborder">
<span class="toolbar-item"> <a href="#" id="toolbar-copy"><spring:message code="toolbar.copy"/></a></span> 
<span class="toolbar-item"> <a href="#" id="toolbar-cut"><spring:message code="toolbar.cut"/></a></span> 
<span class="toolbar-item"> <a href="#" id="toolbar-past"><spring:message code="toolbar.past"/></a></span> 
<span class="toolbar-item"> <a href="#" id="toolbar-rename"><spring:message code="toolbar.rename"/></a></span> 
<span class="toolbar-item"> <a href="#" id="toolbar-delete"><spring:message code="toolbar.delete"/></a></span>
</div> 
 <div class="span-3 colborder">
<span class="toolbar-item"> <a href="#" id="toolbar-zip"><spring:message code="toolbar.zip"/></a></span>
</div>
<div>
<span class="toolbar-item"> <a href="#"	id="toolbar-create"><spring:message code="toolbar.create"/></a></span> 
<span class="toolbar-item" id="file-uploader"></span>    
</div> 
</div>

<div class="span-24 last" id="info-toolbar"></div>

<div class="span-7 colborder">
<div id="fileTree"><spring:message code="error.javascriptengine"/></div>
</div>

<div class="span-8 colborder">
<div id="browserMain"></div>
</div>

</div>

</div>

</div>

</div>

