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
				<div class="row">
					<div class="col-sm-12 col-lg-4">
						<div class="input-group">
							<div class="input-group-btn">
							<button type="button" id="adminFindResearchersByGeneButton"
								class="btn btn-default">
								<span class="glyphicon glyphicon-search"></span>
							</button>
							<button type="button" id="adminResetResearchersButton"
								class="btn btn-default" style="margin-right:10px;">
								<span class="glyphicon glyphicon-repeat"></span>
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
							<span class="input-group-addon">Gene</span> <input type="hidden"
								id="adminSearchGenesSelect" name="adminSearchGenesSelect"
								class="bigdrop form-control">
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
				</div>
				<hr>
				<div class="row">
					<div class="col-md-12">
						<%@ include file="researchersTable.jsp"%>
					</div>
				</div>
			</div>

			

		</div>
	</div>
	<!-- /main-content -->

</div>