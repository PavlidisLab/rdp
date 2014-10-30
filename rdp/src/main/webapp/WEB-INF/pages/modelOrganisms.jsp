<div>
	<div class="main-header">
		<h2>Model Organism Manager</h2>
		<em>Human</em>
	</div>

	<div class="main-content">

		<div id="research-focus" class="row">

			<div class="col-md-12">
				<h3>
					<a href="#"><i class="fa fa-edit yellow-icon"></i></a> Research Focus
				</h3>
				<div class="col-sm-8 research-focus">
					<p custom-placeholder=true data-ph="My research on this organism involves..." class="data-paragraph">My research lies at the intersection
						of bioinformatics and neuroscience. I have a particular interest
						in neuropsychiatric disorders such as schizophrenia and autism,
						and how they affect the function of chemical synapses. A current
						focus of work in my lab involves the large-scale or meta-analysis
						of functional genomics data (e.g. microarrays).</p>
				</div>
			</div>
		</div>

		<!-- NAV TABS -->
		<ul class="nav nav-tabs">
			<li class="active"><a href="#gene-tab" data-toggle="tab"><i
					class="fa fa-link"></i> Genes</a></li>
			<li class=""><a href="#go-tab" data-toggle="tab"><i
					class="fa fa-sitemap"></i> Gene Ontologies</a></li>
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
							<button class="btn btn-danger">
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
							<button class="btn btn-danger">
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