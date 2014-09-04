<div class="alert alert-warning" id="geneManagerFailed" hidden="true">
	<a href="#" class="close" data-dismiss="alert">&times;</a>
	<div id="geneManagerMessage">Failed to load genes.</div>
</div>

<div class="form-group">

	<table id="geneManagerTable" class="table table-bordered stripe"
		cellspacing="0" width="100%">
		<thead>
			<tr>
				<th>Symbol</th>
				<th>Alias</th>
				<th>Name</th>
			</tr>
		</thead>

		<!-- 	<tfoot>
		<tr>
            <th>Symbol</th>
            <th>Alias</th>
            <th>Name</th>
		</tr>
	</tfoot> -->

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

<div class="form-group">
    <button type="button" id="removeGeneButton"
        class="btn btn-default btn-sm" data-toggle="tooltip"
        data-placement="bottom" title="Remove selected gene">
        <span class="glyphicon glyphicon-minus-sign"></span>
    </button>
</div>

<!-- Our scripts -->
<script src="scripts/api/geneManagerTable.js"></script>
