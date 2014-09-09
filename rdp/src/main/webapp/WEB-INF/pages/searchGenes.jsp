<!-- 

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 -->
<div class="form-group">
	<label class="col-sm-2 control-label">Select Gene</label>
	<div class="col-sm-8">
		
			<input type="hidden" class="bigdrop form-control" id="searchGenesSelect" />
		

	</div>
		<button type="button" id="addGeneButton" class="btn btn-default" data-toggle="tooltip" data-placement="bottom" title="Add gene">
			<span>Add</span>
		</button>
</div>
<!-- Our scripts -->
<script src="scripts/api/searchGenes.js"></script>