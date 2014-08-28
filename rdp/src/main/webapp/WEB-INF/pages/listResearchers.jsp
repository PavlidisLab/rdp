<div class="container">

	<div class="alert alert-warning
		id="listResearchersFailed" hidden="true">
		<a href="#" class="close" data-dismiss="alert">&times;</a>
		<div id="listResearchersMessage">Failed loading all researchers.</div>
	</div>

	<div class="row">
	    <label class="col-sm-3">Find researchers by gene </label>
		<input type="hidden" class="bigdrop col-sm-7" id="findResearchersByGenesSelect" />
		<button type="button" id="findResearchersByGeneButton"
			class="btn btn-default">
			<span class="glyphicon glyphicon-search"></span>
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
					<th>Department</th>
				</tr>
			</thead>

			<tfoot>
				<tr>
					<th>Username</th>
					<th>Email</th>
					<th>First name</th>
					<th>Last name</th>
					<th>Organization</th>
					<th>Department</th>
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
</div>

<!-- Our scripts -->
<script src="scripts/api/listResearchers.js"></script>
