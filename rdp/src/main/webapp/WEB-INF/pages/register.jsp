<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Diseases: Models & Mechanisms Network</title>

<!-- Bootstrap core CSS -->
<link href="styles/bootstrap.min.css" rel="stylesheet">

<!-- Optional Bootstrap Theme -->
<link href="data:text/css;charset=utf-8,"
	data-href="styles/bootstrap-theme.min.css" rel="stylesheet"
	id="bs-theme-stylesheet">

<!-- http://www.bootply.com/Wzv3JQreK5     http://ivaynberg.github.io/select2/select2-2.1/select2.css   -->
<link href="styles/select2.css" rel="stylesheet">
<link href="styles/select2-bootstrap.css" rel="stylesheet" />

<link rel="stylesheet" type="text/css"
	href="styles/jquery.dataTables.css">

<link rel="stylesheet" type="text/css"
	href="styles/dataTables.tableTools.css">

<!-- MetisMenu CSS -->
<!-- THIS HAS BEEN ALTERED DO NOT MINIFY WILLY-NILLY -->
<link href="styles/metisMenu/metisMenu.min.css" rel="stylesheet">

<link rel="stylesheet"
	href="styles/font-awesome-4.2.0/css/font-awesome.min.css">

<!-- Custom CSS -->
<link href="styles/rdp.css" rel="stylesheet" title="default" class="themes">

<!-- Custom CSS -->
<link href="styles/rdp_dark.css" rel="alternate stylesheet" title="dark" class="themes" disabled="">

</head>

<body>
	<div class="wrapper">
		<div id="rdp-navbar">

			<nav class="navbar navbar-default navbar-static-top"
				role="navigation">
				<div class="">
					<!-- Brand and toggle get grouped for better mobile display -->
					<div class="navbar-header">
						<button type="button" class="navbar-toggle" data-toggle="collapse"
							data-target="#bs-example-navbar-collapse-1">
							<span class="sr-only">Toggle navigation</span> <span
								class="icon-bar"></span> <span class="icon-bar"></span> <span
								class="icon-bar"></span>
						</button>
						<a class="navbar-brand" href="http://www.rare-diseases-catalyst-network.ca/" target="_blank">Rare Diseases:
							Models & Mechanisms Network</a>
					</div>

					<!-- Collect the nav links, forms, and other content for toggling -->
					<div class=""
						id="bs-example-navbar-collapse-1">
						<!-- right navbar -->
						<ul class="nav navbar-nav navbar-right">

							<!-- username will be added here when the user has logged in -->
							<li class="dropdown"><a href="#" class="dropdown-toggle"
								data-toggle="dropdown"><span id="navbar-username"></span> <i
									class="fa fa-user fa-fw"></i> <i
									class="fa fa-caret-down"></i>
							</a>
								<ul class="dropdown-menu dropdown-user">
									<li><a id="profileDropdown" href="#myAccount"><i
											class="fa fa-user fa-fw"></i> User Profile</a></li>
									<li><a id="settingsDropdown" href="#settings-tab"><i
											class="fa fa-gear fa-fw"></i> Settings</a></li>
									<li class="divider"></li>
									<li><a id="logout" href="#logout"><i
											class="fa fa-sign-out fa-fw"></i> Logout</a></li>
								</ul>
						</ul>
					</div>
					<!-- /.navbar-collapse -->






				</div>
				<!-- /.container-fluid -->
			</nav>
		</div>
		<div class="content-wrapper clearfix">
			<aside class="sidebar">
				<nav class="sidebar-nav">
					<ul id="menu">
						<li style="display: none;"><a href="#admin" data-toggle="tab">
								<span class="sidebar-nav-item-icon fa fa-fw fa-cogs fa-lg"></span>
								<span class="sidebar-nav-item">Administration</span>
						</a></li>
						<li><a href="#overview" data-toggle="tab">
								<span class="sidebar-nav-item-icon fa fa-fw fa-home fa-lg"></span>
								<span class="sidebar-nav-item">Overview</span>
						</a></li>
						<li class="active active-metis"><a href="#myAccount" data-toggle="tab"> <span
								class="sidebar-nav-item-icon fa fa-fw fa-user fa-lg"></span> <span
								class="sidebar-nav-item">My Account</span>
						</a></li>
						<li id="myModelOrganismsList"><a href="#modelOrganisms" data-toggle="tab"> <span
								class="sidebar-nav-item-icon fa fa-fw fa-bug fa-lg"></span> <span
								class="sidebar-nav-item">My Model Organisms</span> <span
								class="fa arrow"></span>
						</a>
							<ul>
								<li><a href="#modelOrganisms">Human</a></li>
								<li><a href="#modelOrganisms">Mouse</a></li>
								<li><a href="#modelOrganisms">Rat</a></li>
								<li><a href="#modelOrganisms">Yeast</a></li>
								<li><a href="#modelOrganisms">Zebrafish</a></li>
								<li><a href="#modelOrganisms">Fruit Fly</a></li>
								<li><a href="#modelOrganisms">Roundworm</a></li>
								<li><a href="#modelOrganisms">E. Coli</a></li>
								<li><a href="#addOrganism">Add Organism<span
										class="fa arrow"></span></a>
									<ul class="sub-list">
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Human</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Mouse</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Rat</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Yeast</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Zebrafish</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Fruit Fly</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; Roundworm</a>
										<li><a href="#add"><i
												class="fa fa-plus-circle green-icon"></i>&nbsp; E. Coli</a>
										<li style="display: none;"><a href="#"><i
												class="fa fa-ban red-icon"></i>&nbsp; No More Organisms</a>
									</ul></li>
							</ul></li>
						<li><a href="#help" data-toggle="tab"> <span
								class="sidebar-nav-item-icon fa fa-fw fa-question-circle fa-lg"></span>
								<span class="sidebar-nav-item">Help</span>
						</a></li>

					</ul>
				</nav>
				<div class="sidebar-footer">
					<p>Models &amp; Mechanisms Network</p>
					<h1>RARE DISEASES</h1>
				</div>
			</aside>

			<section class="content">
				<div class="tab-content">
					<div class="tab-pane" id="overview">

						<div class="col-xs-12">
							<div class="row">
								<div class="col-lg-4 ">
									<ul class="breadcrumb">
										<li class="active"><i class="fa fa-home"></i><a
											href="#overview">Overview</a></li>
									</ul>
								</div>

							</div>
							<%@ include file="overview.jsp"%>
						</div>

					</div>

					<div class="tab-pane active" id="myAccount">

						<div class="col-xs-12">
							<div class="row">
								<div class="col-lg-4 ">
									<ul class="breadcrumb">
										<li class="active"><i class="fa fa-user"></i><a
											href="#myAccount">My Account</a></li>
									</ul>
								</div>

							</div>
							<%@ include file="profile.jsp"%>
						</div>

					</div>

					<div class="tab-pane" id="modelOrganisms">

						<div class="col-xs-12">
							<div class="row">
								<div class="col-lg-4 ">
									<ul class="breadcrumb">
										<li><i class="fa fa-bug"></i><a href="#">Model
												Organisms</a></li>
										<li class="active"><a id="currentOrganismBreadcrumb"
											href="#modelOrganisms"></a></li>
									</ul>
								</div>

							</div>
							<%@ include file="modelOrganisms.jsp"%>
						</div>

					</div>

					<div class="tab-pane" id="admin">

						<div class="col-xs-12">
							<div class="row">
								<div class="col-lg-4 ">
									<ul class="breadcrumb">
										<li class="active"><i class="fa fa-cogs"></i><a
											href="#admin">Administration</a></li>
									</ul>
								</div>

							</div>
							<%@ include file="admin.jsp"%>
						</div>

					</div>

					<div class="tab-pane" id="help">

						<div class="col-xs-12">
							<div class="row">
								<div class="col-lg-4 ">
									<ul class="breadcrumb">
										<li class="active"><i class="fa fa-question-circle"></i><a
											href="#help">Help</a></li>
									</ul>
								</div>

							</div>
						</div>
						
					</div>

				</div>

			</section>
		</div>
		<div class="push-sticky-footer"></div>
	</div>
	<%@ include file="addGenesModal.jsp"%>
	<%@ include file="addTermsModal.jsp"%>
	<%@ include file="confirmModal.jsp"%>
	<footer class="footer"> © Copyright 2014. "Rare Diseases: Models & Mechanisms Network" All rights reserved. </footer>


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

	<!-- Metis Menu Plugin JavaScript -->
	<!-- THIS HAS BEEN ALTERED DO NOT MINIFY WILLY-NILLY -->
	<script src="scripts/lib/metisMenu/metisMenu.js"></script>

	<script src="scripts/api/utility.js?version=5"></script>
	<script src="scripts/api/researcherModel.js?version=5"></script>
	<script src="scripts/api/profile.js?version=5"></script>
	<script src="scripts/api/settings.js?version=5"></script>
	<script src="scripts/api/modelOrganisms.js?version=5"></script>
	<script src="scripts/api/geneManager.js?version=5"></script>
	<script src="scripts/api/goManager.js?version=5"></script>
	<script src="scripts/api/admin.js?version=5"></script>
	<script src="scripts/api/register.js?version=5"></script>
	<script src="scripts/api/navbar.js?version=5"></script>

</body>
</html>