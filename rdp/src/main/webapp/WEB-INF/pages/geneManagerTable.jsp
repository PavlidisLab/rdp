<div class="form-group" >
	<div class="col-sm-offset-1 col-sm-10" style="padding-bottom:10px">
	    <button type="button" id="removeGeneButton"
	        class="btn btn-default btn-xs pull-left" data-toggle="tooltip"
	        data-placement="bottom" title="Remove selected gene">
	        <span>Remove Selected Genes</span>
	    </button>
    </div>
</div>

<div class="form-group">
	<div class="col-sm-offset-1 col-sm-10">
	<table id="geneManagerTable" class="table table-bordered stripe text-left"
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
</div>

<!-- Our scripts -->
<script src="scripts/api/geneManagerTable.js"></script>
