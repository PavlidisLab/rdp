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
		<div class="col-md-9">
		<a data-toggle="collapse" href="#collapseReadFirst" class="read-first red-link" aria-expanded="true">Read this first - how to decide which genes to add</a>
		</div>
		<div class="panel col-md-9">
			<div id="collapseReadFirst" class="panel-collapse collapse in" aria-expanded="true">
				<div class="panel-body"> 
					<p>Use this Manager to enter genes you study. It is important that you choose the genes carefully. Complete instructions are available <a href="#" onclick='return utility.openAccordian($("#collapse8"))'>here</a>. Briefly:</p>
					<ol>
					<li>
					<p>From the Genes tab, click "Add genes" to find and add genes to your profile. You should indicate which of your genes are Primary and which are non-Primary:</p>
					<ul>
					<li>A <b>Primary (or Tier 1) gene</b> is one you are able to immediately, specifically and rapidly study in your laboratory. Most registrants will have between one and ten primary genes.</li>
					<li><b>Non-Primary (Tier 2) gene</b> are those you do not consider primary but would be able to work on rapidly and specifically with minimal set-up time. For most registrants we expect there might be between one and 100 non-Primary genes entered.</li>
					</ul>
					</li>
					<li>Click the "Gene Ontology Terms" tab and click "Add GO Term(s)". By indicating the GO terms most related to your work, genes having those annotations will be considered as Tier 3 genes.</li>
					</ol>
					<p><b>All information provided is <a href="#" onclick='return utility.openAccordian($("#collapse4"))'>confidential.</a></b></p>
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