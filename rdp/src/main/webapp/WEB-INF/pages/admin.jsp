<div>
	<div class="main-header">
		<h2>Administration</h2>
		<em>Utilities</em>
	</div>

	<div class="main-content">

		<!-- NAV TABS -->
		<ul id="admin-nav-tabs" class="nav nav-tabs">
			<li class="active"><a href="#researchers-tab" data-toggle="tab"><i
					class="fa fa-group"></i> All Researchers</a></li>
		</ul>
		<!-- END NAV TABS -->

		<div class="tab-content admin-page">
			<div class="tab-pane active" id="researchers-tab">
				<div class="panel-group" id="admin-accordion" role="tablist"
					aria-multiselectable="true">
					<div class="panel panel-default">
						<div data-toggle="collapse" data-target="#collapse-basic"
							data-parent="#admin-accordion" aria-expanded="true"
							aria-controls="collapse-basic" class="panel-heading" role="tab"
							id="basic-heading">
							<h4 class="panel-title">
								<a href="#"> Basic Search: </a>
							</h4>
						</div>
						<div id="collapse-basic" class="panel-collapse collapse in"
							role="tabpanel" aria-labelledby="basic-heading">
							<div class="panel-body">
				<div class="row">
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<div class="input-group-btn">
							<button type="button" id="adminFindResearchersByGeneButton"
								class="btn btn-default">
								<span class="glyphicon glyphicon-search"></span>
							</button>
							</div>
							<span class="input-group-addon">Organism</span> <select
								id="adminTaxonCommonNameSelect" class="form-control">
								<option value="9606">Human</option>
								<option value="10090">Mouse</option>
								<option value="10116">Rat</option>
								<option value="559292">Yeast</option>
								<option value="7955">Zebrafish</option>
								<option value="7227">Fruit Fly</option>
								<option value="6239">Roundworm</option>
								<option value="562">E. Coli</option>
							</select>
						</div>
					</div>

					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Tier</span> <select
								id="adminTierSelect" class="form-control">
								<option value="">Any</option>
								<option value="TIER1">Tier 1</option>
								<option value="TIER2">Tier 2</option>
							</select>
						</div>
					</div>
					
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Gene</span> <input type="hidden"
								id="adminSearchGenesSelect" name="adminSearchGenesSelect"
								class="bigdrop form-control">
						</div>
					</div>
				</div>
							</div>
						</div>
					</div>
					
					<div class="panel panel-default">
						<div data-toggle="collapse" data-target="#collapse-advanced"
							data-parent="#admin-accordion" aria-expanded="false"
							aria-controls="collapse-advanced" class="panel-heading" role="tab"
							id="advanced-heading">
							<h4 class="panel-title">
								<a href="#"> Advanced Search: </a>
							</h4>
						</div>
						<div id="collapse-advanced" class="panel-collapse collapse"
							role="tabpanel" aria-labelledby="advanced-heading">
							<div class="panel-body">
				<div class="row" id="admin-advanced">
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<div class="input-group-btn">
							<button type="button" class="btn btn-default submit">
								<span class="glyphicon glyphicon-search"></span>
							</button>
							</div>
							<span class="input-group-addon">Organism</span> <select class="form-control taxon">
								<option value="9606">Human</option>
								<option value="10090">Mouse</option>
								<option value="10116">Rat</option>
								<option value="559292">Yeast</option>
								<option value="7955">Zebrafish</option>
								<option value="7227">Fruit Fly</option>
								<option value="6239">Roundworm</option>
								<option value="562">E. Coli</option>
							</select>
						</div>
					</div>
					
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Tier</span> <select class="form-control tier">
								<option value="">Any</option>
								<option value="TIER1">Tier 1</option>
								<option value="TIER2">Tier 2</option>
							</select>
						</div>
					</div>

					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Symbol</span> <input type="text" class="form-control symbol-pattern" placeholder="Symbol regex...">
						</div>
					</div>
					
				</div>
				
				<hr/>
				
				<div class="row" id="admin-advanced-term" style="display:none;">
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<div class="input-group-btn">
							<button type="button" class="btn btn-default submit">
								<span class="glyphicon glyphicon-search"></span>
							</button>
							</div>
							<span class="input-group-addon">Organism</span> <select class="form-control taxon">
								<option value="9606">Human</option>
								<option value="10090">Mouse</option>
								<option value="10116">Rat</option>
								<option value="559292">Yeast</option>
								<option value="7955">Zebrafish</option>
								<option value="7227">Fruit Fly</option>
								<option value="6239">Roundworm</option>
								<option value="562">E. Coli</option>
							</select>
						</div>
					</div>

					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Term Name</span> <input type="text" class="form-control term-pattern" placeholder="Term regex...">
						</div>
					</div>
					
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<span class="input-group-addon">Term ID</span> <input type="text" class="form-control id-pattern" placeholder="ID regex...">
						</div>
					</div>
					
				</div>
							</div>
						</div>
					</div>
					
				</div>
				
				<hr>
				<div class="row">
					<div class="col-md-12">
						<div class="alert alert-warning col-sm-12" hidden="true">
							<a href="#" class="close" data-hide="alert">&times;</a>
							<div class="text-center"></div>
						</div>
						<%@ include file="researchersTable.jsp"%>
					</div>
				</div>
			</div>

			

		</div>
	</div>
	<!-- /main-content -->

</div>