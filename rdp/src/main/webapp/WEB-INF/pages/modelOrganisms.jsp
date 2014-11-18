<div>
	<div class="main-header">
		<h2>Model Organism Manager</h2>
		<em>Human</em>
		<div class="alert alert-warning col-sm-12" hidden="true">
			<a href="#" class="close" data-hide="alert">&times;</a>
			<div class="text-center"></div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-4">
		<a data-toggle="collapse" href="#collapseReadFirst" class="read-first" >Read this first - how to select genes</a>
		</div>
		<div class="panel col-md-9">
			<div id="collapseReadFirst" class="panel-collapse collapse">
				<div class="panel-body"> 
					<p>Use this Manager to enter genes you study. It is important that you choose the genes carefully. Your 
					genes are divided into three Tiers.</p>
					<ul>
						<li><b>Tier 1 or "Primary" genes</b> are those you currently
							work on in a model organism. We
							expect this to typically be between one and ten genes.</li>
						<li><b>Tier 2 genes</b> are those you do not mark as
							"Primary". For
							most registrants we expect there might be between one and 100
							non-Primary genes entered.</li>
						<li><b>Tier 3 genes</b> are not specifically selected. Instead, we use Gene Ontology (GO) terms selected to
							infer them. By indicating the GO terms most related to your work,
							genes having those annotations will be considered as Tier 3
							genes.</li>
					</ul>
				<p>For more information, see the <a href="#" onclick='return utility.openAccordian($("#collapse8"))'>Help</a></p>

				</div>
			</div>
		</div>

	</div>


	<div class="main-content">

		<div id="research-focus" class="row">

			<div class="col-md-12">
				<h3>
					<a href="#" data-toggle="tooltip" data-placement="bottom"
						title="Edit"><i class="fa fa-edit yellow-icon"></i></a> Organism
					Research Focus
				</h3>
				<div class="col-sm-8 research-focus">
					<p custom-placeholder=true
						data-ph="My research on this organism involves..."
						class="data-editable data-paragraph well"></p>
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