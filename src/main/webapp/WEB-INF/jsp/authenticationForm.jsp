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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>


<div id="jqueryFileTree" >

  <form:form method="post" id="authenticationForm">

	<input type="hidden" id="currentDir" value="${currentDir}"/>
	
	<label for="username"><spring:message code="auth.username"/></label>
	<input type="text" id="username" value="${username}"/>
	
	<label for="password"><spring:message code="auth.password"/></label>
	<input type="password" id="password" value="${password}"/>

    <a id="authenticate" href="" onclick="return false;">
      <spring:message code="auth.submit"/>
    </a>
 
  </form:form>


<script type="text/javascript">

  (function ($) {
    $('#authenticate').bind('click', function() {
      $.authenticate($("#currentDir").val(), $("#username").val(), $("#password").val());
    });
    
    $('#username').keyup(function(e) {
  	  $.hideInfoToolBar();
    });
    
   $('#password').keyup(function(e) {
	  $.hideInfoToolBar();
      if(e.keyCode == 13) {
         $.authenticate($("#currentDir").val(), $("#username").val(), $("#password").val());
      }
    });
            
   })(jQuery);
    
    
</script>
   
</div>
    
