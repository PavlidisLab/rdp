
<div class="form-group">
	<label class="col-sm-2 control-label">Bulk Upload</label>
	<div class="col-sm-9">
		<textarea class="form-control" rows="5" id="importGeneSymbolsTextArea"
			placeholder="Enter one gene symbol per line up to a maximum of 1000 genes"></textarea>
	</div>
</div>

<div class="form-group">
	<div class="col-sm-3 pull-right">
		<button type="button" id="importGenesButton"
			class="btn btn-default btn-sm" data-toggle="tooltip"
			data-placement="bottom" title="Bulk Upload Genes">
			<span>Add All</span>
		</button>
	
		<button type="button" id="clearImportGenesButton"
			class="btn btn-default btn-sm" data-toggle="tooltip"
			data-placement="bottom" title="Clear Bulk Upload">
			<span>Clear</span>
		</button>
		
<!-- 		<div id="spinImportGenesButton" hidden="true">
			<span class="glyphicon glyphicon-refresh animate"></span>
		</div> -->
	</div>
</div>

<!-- Our scripts 
	<script src="scripts/api/importGenes.js"></script>-->