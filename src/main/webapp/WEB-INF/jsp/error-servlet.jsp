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
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="fr" lang="fr">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>esup-servlet-stockage</title>

<!-- Framework CSS -->
<link rel="stylesheet"
	href="/esup-portlet-stockage/css/blueprint/screen.css" type="text/css"
	media="screen, projection">

<link rel="stylesheet"
	href="/esup-portlet-stockage/css/blueprint/print.css" type="text/css"
	media="print">

<link rel="stylesheet" href="/esup-portlet-stockage/css/esup-stock.css"
	type="text/css" media="screen, projection">


</head>

<body>


	<div class="esupstock">

		<div class="container">

			<div class="portlet-section">

				<div class="portlet-section-body">

					<spring:message code="error.general" />

					<hr/>
					${exception.message}

				</div>

			</div>

		</div>

	</div>


</body>

</html>
