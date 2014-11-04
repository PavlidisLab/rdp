<!-- Modal -->
<div id="addGenesModal" class="modal fade" tabindex="-1" role="dialog"
	aria-labelledby="myModalLabel" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
				</button>
				<h5 class="modal-title" id="myModalLabel">Add Genes</h5>
			</div>
			<div class="modal-body">
				<form class="form-horizontal" role="form">

					<div class="form-group">
						<label for="searchGenesSelect" class="col-sm-3 control-label">Select Gene</label>
						<div class="col-sm-9">
							<input type="text" id="searchGenesSelect" name="searchGenesSelect"
								class="bigdrop form-control">
						</div>
					</div>
					
					<div class="form-group">
						<div class="col-sm-2 pull-right">
						<button type="button" id="addGeneButton" class="btn btn-default btn-sm"
							data-toggle="tooltip" data-placement="bottom" title="Add gene">
							<span><i class="fa fa-plus-circle green-icon"></i>&nbsp; Add</span>
						</button>
						</div>
					</div>
					
					<hr>


					<div class="form-group">
						<label class="col-sm-3 control-label">Bulk Upload</label>
						<div class="col-sm-9">
							<textarea class="form-control" rows="5"
								id="importGeneSymbolsTextArea"
								placeholder="Enter one gene symbol per line up to a maximum of 1000 genes"></textarea>
						</div>
					</div>

					<div class="form-group">
						<div class="col-sm-4 pull-right">
							<button type="button" id="importGenesButton"
								class="btn btn-default btn-sm" data-toggle="tooltip"
								data-placement="bottom" title="Bulk Upload Genes">
								<span><i class="fa fa-upload green-icon"></i>&nbsp; Add All</span>
							</button>

							<button type="button" id="clearImportGenesButton"
								class="btn btn-default btn-sm" data-toggle="tooltip"
								data-placement="bottom" title="Clear Bulk Upload">
								<span><i class="fa fa-trash red-icon"></i>&nbsp; Clear</span>
							</button>

						</div>
					</div>


				</form>
			</div>
			<div class="modal-footer"></div>
		</div>
	</div>
</div>
