<div class="container">

	<div class="alert alert-warning
		id="
		listResearchersFailed" hidden="true">
		<a href="#" class="close" data-hide="alert">&times;</a>
		<div id="listResearchersMessage">Failed loading all researchers.</div>
	</div>

	<div class="row">
		<label class="col-sm-3">Find researchers by gene </label>
		<div class="col-sm-2">
			<select id="taxonCommonNameSelectListResearchers"
				class="form-control">
				<!-- 
                <option>Human</option>
                <option>Mouse</option>
                <option>Rat</option>
				 -->
			</select>
		</div>
		<div class="col-sm-6">
			<input type="hidden" class="bigdrop form-control"
				id="findResearchersByGenesSelect" />
		</div>
		<button type="button" id="findResearchersByGeneButton"
			class="btn btn-default">
			<span class="glyphicon glyphicon-search"></span>
		</button>
		<button type="button" id="resetResearchersButton"
			class="btn btn-default">
			<span class="glyphicon glyphicon-repeat"></span>
		</button>
	</div>

	<!-- empty filler -->
	<p>
	<div class="row">
		<table id="listResearchersTable"
			class="table table-striped table-bordered" cellspacing="0"
			width="100%">
			<thead>
				<tr>
					<th>Username</th>
					<th>Email</th>
					<th>First name</th>
					<th>Last name</th>
					<th>Organization</th>
					<th>Genes Count</th>
					<th>Tier</th>
				</tr>
			</thead>

			<tfoot>
				<tr>
					<th>Username</th>
					<th>Email</th>
					<th>First name</th>
					<th>Last name</th>
					<th>Organization</th>
					<th>Genes Count</th>
					<th>Tier</th>
				</tr>
			</tfoot>

			<tbody>
				<!--  
        <tr>
                <td>testUsername</td>
                <td>testEmail</td>
                <td>testFirstname</td>
                <td>testLastname</td>
                <td>testOrganization</td>
                <td>testDepartment</td>
            </tr>
        -->
			</tbody>
		</table>
	</div>
	
	<div class="row">
		<button type="button" id="updateCache"
			class="btn btn-danger">
			<span class="">Update Cache</span>
		</button>
	</div>
	
</div>

<!-- Our scripts -->

