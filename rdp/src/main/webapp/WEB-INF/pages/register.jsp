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

<link rel="stylesheet" type="text/css"
	href="styles/jquery.dataTables.css">

<link rel="stylesheet" type="text/css"
	href="styles/dataTables.tableTools.css">

<!-- MetisMenu CSS -->
<link href="styles/metisMenu/metisMenu.min.css" rel="stylesheet">

<link rel="stylesheet"
	href="styles/font-awesome-4.2.0/css/font-awesome.min.css">



<!-- Custom CSS -->
<link href="styles/simple-sidebar.css" rel="stylesheet">

</head>

<body>

	<div>

		<nav class="navbar navbar-default navbar-static-top" role="navigation">
			<div class="container-fluid">
				<!-- Brand and toggle get grouped for better mobile display -->
				<div class="navbar-header">
					<button type="button" class="navbar-toggle" data-toggle="collapse"
						data-target="#bs-example-navbar-collapse-1">
						<span class="sr-only">Toggle navigation</span> <span
							class="icon-bar"></span> <span class="icon-bar"></span> <span
							class="icon-bar"></span>
					</button>
					<a class="navbar-brand" href="register.html">Rare Diseases: Models & Mechanisms Network</a>
				</div>

				<!-- Collect the nav links, forms, and other content for toggling -->
				<div class="collapse navbar-collapse"
					id="bs-example-navbar-collapse-1">
					<!-- right navbar -->
					<ul class="nav navbar-nav navbar-right">

						<!-- username will be added here when the user has logged in -->
						<li class="dropdown"><a href="#" class="dropdown-toggle"
							data-toggle="dropdown"> <i
								class="fa fa-user fa-fw fa-inverse"></i> <i
								class="fa fa-caret-down fa-inverse"></i>
						</a>
							<ul class="dropdown-menu dropdown-user">
								<li><a href="#profile" data-toggle="tab"><i class="fa fa-user fa-fw"></i> User
										Profile</a></li>
								<li><a id="settingsDropdown" href="#settings-tab"><i class="fa fa-gear fa-fw"></i>
										Settings</a></li>
								<li class="divider"></li>
								<li><a id="logout" href="login.html"><i
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
					<li class="activeMetis"><a href="#overview" data-toggle="tab">
							<span class="sidebar-nav-item-icon fa fa-fw fa-home fa-lg"></span>
							<span class="sidebar-nav-item">Overview</span>
					</a>

					
					</li>
					<li><a href="#profile" data-toggle="tab"> <span
							class="sidebar-nav-item-icon fa fa-fw fa-user fa-lg"></span> <span
							class="sidebar-nav-item">Profile</span>
					</a>

					</li>
					<li><a href="#"> <span
							class="sidebar-nav-item-icon fa fa-fw fa-bug fa-lg"></span> <span
							class="sidebar-nav-item">Model Organisms</span> <span
							class="fa arrow"></span>
					</a>
						<ul>
							<li><a href="#modelOrganisms" data-toggle="tab">Human</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Mouse</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Rat</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Yeast</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Zebrafish</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Fruit Fly</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">Roundworm</a></li>
							<li><a href="#modelOrganisms" data-toggle="tab">E. Coli</a></li>
						</ul></li>

				</ul>
			</nav>
		</aside>

		<section class="content">
			<div class="tab-content">
				<div class="tab-pane active" id="overview">

					<div class="col-xs-12">
						<div class="row">
							<div class="col-lg-4 ">
								<ul class="breadcrumb">
									<li class="active"><i class="fa fa-home"></i><a
										href="#overview">Overview</a></li>
								</ul>
							</div>

						</div>
					</div>

				</div>

				<div class="tab-pane" id="profile">

					<div class="col-xs-12">
						<div class="row">
							<div class="col-lg-4 ">
								<ul class="breadcrumb">
									<li class="active"><i class="fa fa-user"></i><a
										href="#profile">Profile</a></li>
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
									<li><i class="fa fa-bug"></i><a href="#">Model Organisms</a></li>
									<li class="active"><a id="currentOrganismBreadcrumb" href="#modelOrganisms"></a></li>
								</ul>
							</div>

						</div>
						<%@ include file="modelOrganisms.jsp"%>
					</div>

				</div>

			</div>

		</section>
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

	<!-- Metis Menu Plugin JavaScript -->
	<script src="scripts/lib/metisMenu/metisMenu.js"></script>

	<script src="scripts/api/utility.js?version=2"></script>
	<script src="scripts/api/researcherModel.js?version=2"></script>
	<script src="scripts/api/profile.js?version=2"></script>
	<script src="scripts/api/settings.js?version=2"></script>
	<script src="scripts/api/modelOrganisms.js?version=2"></script>
	<script src="scripts/api/geneManager.js?version=2"></script>
	<script src="scripts/api/goManager.js?version=2"></script>
	<script src="scripts/api/register.js?version=2"></script>
	<script src="scripts/api/navbar.js?version=2"></script>

	<script>
      $( function() {

         $( '#menu' ).metisMenu();
      } );
   </script>

</body>
</html>