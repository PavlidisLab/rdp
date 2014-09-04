
<div class="form-group">
	<textarea class="form-control" rows="5" id="importGeneSymbolsTextArea"
		placeholder="Enter one gene symbol per line up to a maximum of 1000 genes"></textarea>
</div>

<div class="form-group">
	<button type="button" id="importGenesButton"
		class="btn btn-default btn-sm" data-toggle="tooltip"
		data-placement="bottom" title="Import gene symbols">
		<span class="glyphicon glyphicon-import"></span>
	</button>

	<button type="button" id="clearImportGenesButton"
		class="btn btn-default btn-sm" data-toggle="tooltip"
		data-placement="bottom" title="Remove all gene symbols">
		<span class="glyphicon glyphicon-trash"></span>
	</button>
</div>

<!-- Our scripts -->
<script src="scripts/api/importGenes.js"></script>