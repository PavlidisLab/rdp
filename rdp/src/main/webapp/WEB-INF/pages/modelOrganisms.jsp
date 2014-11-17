<div>
	<div class="main-header">
		<h2>Model Organism Manager</h2>
		<em>Human</em>
		<div class="alert alert-warning col-sm-12" hidden="true">
			<a href="#" class="close" data-hide="alert">&times;</a>
			<div class="text-center"></div>
		</div>
	</div>

	<div class="main-content">

		<div id="research-focus" class="row">

			<div class="col-md-12">
				<h3>
					<a href="#" data-toggle="tooltip" data-placement="bottom" title="Edit"><i class="fa fa-edit yellow-icon"></i></a> Organism Research Focus
				</h3>
				<div class="col-sm-8 research-focus">
					<p custom-placeholder=true data-ph="My research on this organism involves..." class="data-editable data-paragraph well"></p>
				</div>
			</div>
		</div>

		<!-- NAV TABS -->
		<ul class="nav nav-tabs">
			<li class="active"><a href="#gene-tab" data-toggle="tab"><i
					class="fa fa-link"></i> Genes</a></li>
			<li class=""><a href="#go-tab" data-toggle="tab"><i
					class="fa fa-sitemap"></i> Gene Ontology Terms</a></li>
		</ul>
		<!-- END NAV TABS -->

		<div class="tab-content modelOrganism-page">
			<!-- GENE TAB CONTENT -->
			<div class="tab-pane gene-manager active" id="gene-tab">
				<div class="row">
					<div class="col-md-12">
						<%@ include file="geneTable.jsp"%>
						<hr>
						<div class="col-md-9">
							<button id="gene-tab-save" class="btn btn-danger">
								<i class="fa fa-floppy-o"></i> Save Changes
							</button>
						</div>
					</div>
				</div>				
			</div>
			<!-- END GENE TAB CONTENT -->

			<!-- GO TAB CONTENT -->
			<div class="tab-pane go-manager" id="go-tab">
				<div class="row">
					<div class="col-md-12">
						<%@ include file="goTable.jsp"%>
						<hr>
						<div class="col-md-9">
							<button id="term-tab-save" class="btn btn-danger">
								<i class="fa fa-floppy-o"></i> Save Changes
							</button>
						</div>
					</div>
				</div>
			</div>
			<!-- END GO TAB CONTENT -->
		</div>

	</div>
	<!-- /main-content -->
</div>