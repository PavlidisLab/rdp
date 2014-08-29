<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Disease Project</title>

<!-- Bootstrap core CSS -->
<link href="styles/bootstrap.min.css" rel="stylesheet">

<!-- Optional Bootstrap Theme -->
<link href="data:text/css;charset=utf-8,"
	data-href="styles/bootstrap-theme.min.css" rel="stylesheet"
	id="bs-theme-stylesheet">

<!-- http://www.bootply.com/Wzv3JQreK5     http://ivaynberg.github.io/select2/select2-2.1/select2.css   -->
<link href="styles/select2.css" rel="stylesheet">
<link href="styles/select2-bootstrap.css" rel="stylesheet" />

<!-- DataTables CSS -->
<!-- 
<link rel="stylesheet" type="text/css"
    href="//cdn.datatables.net/1.10.1/css/jquery.dataTables.css">
 -->
<!-- 
 
<link rel="stylesheet" type="text/css"
    href="styles/dataTables.bootstrap.css">
    
     -->
<link rel="stylesheet" type="text/css"
	href="styles/jquery.dataTables.css">

</head>

<body id="register">

	<div id="content">

		<!-- Navigation bar -->
		<%@ include file="navbar.jsp"%>

		<!-- Nav tabs -->
		<ul id="registerTab" class="nav nav-tabs" role="tablist">

			<li><a href="#modelOrganism" role="tab" data-toggle="tab">Model
					organism</a></li>

			<li><a href="#registeredResearchers" role="tab"
				data-toggle="tab" style="display: none;">Registered researchers</a></li>

		</ul>

		<!-- Tab panes -->
		<div class="tab-content">
			<div class="tab-pane active" id="modelOrganism">
				<br />
				<%@ include file="modelOrganism.jsp"%>
			</div>

			<div class="tab-pane" id="registeredResearchers">
				<br />
				<%@ include file="listResearchers.jsp"%>
			</div>

		</div>

	</div>


	<!-- include jQuery, and our script file -->
	<script src="scripts/lib/jquery-1.11.1.js"></script>
	<script src="scripts/lib/bootstrap.min.js"></script>
	<script src="scripts/lib/jquery.validate.min.js"></script>
	<script src="scripts/lib/jquery.json-2.4.js"></script>
	<!-- http://www.bootply.com/Wzv3JQreK5     http://ivaynberg.github.io/select2/select2-2.1/select2.css   -->
	<script src="scripts/lib/select2.js"></script>
	<!-- from http://www.datatables.net/examples/styling/bootstrap.html -->
	<script type="text/javascript" charset="utf8"
		src="scripts/lib/jquery.dataTables.min.js"></script>

	<!-- DataTables -->
	<!-- 
    <script type="text/javascript" charset="utf8"
        src="scripts/lib/dataTables.bootstrap.js"></script>
 -->

	<!-- Our scripts -->
	<script src="scripts/api/register.js"></script>

</body>
</html>