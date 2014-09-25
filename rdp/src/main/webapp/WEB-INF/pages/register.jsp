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

<link rel="stylesheet" type="text/css"
	href="styles/dataTables.tableTools.css">

<!-- Bootstrap core CSS -->
<link href="styles/rdp.css" rel="stylesheet">

<style>
#overview .modal .modal-body {
    max-height: 420px;
    overflow-y: auto;
}
textarea { resize:none; }
/* #geneManagerTable td { white-space: nowrap; } */

.shadowboxedin { 
	border-bottom: 2px solid grey;
	padding:0 0 2% 0;
	margin:0 0 2% 0;
}

.boxedin {
	border-top: 2px solid grey; 
	border-bottom: 2px solid grey;
	padding:2% 0 2% 0;
	margin:2% 0 2% 0;
	}

h3 {text-decoration: underline;}

</style>

</head>

<body id="register">

	<div id="content">

		<!-- Navigation bar -->
		<%@ include file="navbar.jsp"%>

		<div id="container">
		<!-- Nav tabs -->
		<ul id="registerTab" class="nav nav-tabs" role="tablist">

			<li class="active"><a href="#overview" role="tab" data-toggle="tab">Overview</a></li>

			<li><a href="#registeredResearchers" role="tab"
				data-toggle="tab" style="display: none;">Registered researchers</a></li>

		</ul>

		<!-- Tab panes -->
		<div class="tab-content">
 			<div class="tab-pane active" id="overview">
				<br />
				<%@ include file="overview.jsp"%>
			</div>

			<div class="tab-pane" id="registeredResearchers">
				<br />
				<%@ include file="listResearchers.jsp"%>
			</div>

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
	<script src="scripts/lib/dataTables.tableTools.js"></script>
	
	<!-- DataTables -->
	<!-- 
    <script type="text/javascript" charset="utf8"
        src="scripts/lib/dataTables.bootstrap.js"></script>
 -->

	<!-- Our scripts -->
	<script src="scripts/api/researcherModel.js"></script>
	<script src="scripts/api/overview.js"></script>
	<script src="scripts/api/editGenes.js"></script>
	<script src="scripts/api/listResearchers.js"></script>
	<script src="scripts/api/register.js"></script>
	<script src="scripts/api/navbar.js"></script>
	<script src="scripts/api/editProfile.js"></script>
	
	<script src="scripts/api/editUser.js"></script>
	

</body>
</html>