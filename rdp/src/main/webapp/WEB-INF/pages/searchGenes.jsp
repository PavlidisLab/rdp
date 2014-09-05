<!-- 

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 -->
<div class="input-group">
	
	<input type="hidden" class="bigdrop form-control" id="searchGenesSelect" />

	<span class="input-group-btn">
		<button type="button" id="addGeneButton" class="btn btn-default" data-toggle="tooltip" data-placement="bottom" title="Add gene">
			<span class="glyphicon glyphicon-plus-sign"></span>
		</button>
	</span>
</div>

<!-- Our scripts -->
<script src="scripts/api/searchGenes.js"></script>