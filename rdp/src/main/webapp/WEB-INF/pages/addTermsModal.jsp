<!-- Modal -->
<div id="addTermsModal" class="modal fade" tabindex="-1" role="dialog"
	aria-labelledby="myModalLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
				</button>
				<h5 class="modal-title" id="myModalLabel">Add Gene Ontology Term</h5>
			</div>
			<div class="modal-body">
				<div id="addTermsForm" class="custom-transition">
					<form class="form-horizontal" role="form">

						<div class="form-group">
							<label for="searchTermsSelect" class="col-sm-2 control-label">Search for Term</label>
							<div class="col-sm-8">
								<input type="text" id="searchTermsSelect"
									name="searchTermsSelect" class="bigdrop form-control">
							</div>
							<div class="col-sm-2 pull-right">
								<button type="button" id="addTermButton"
									class="btn btn-default btn-sm" data-toggle="tooltip"
									data-placement="bottom" title="Add gene">
									<span><i class="fa fa-plus-circle green-icon"></i>&nbsp;
										Add</span>
								</button>
							</div>
						</div>

						<hr>


						<div class="form-group">
							<div class="col-sm-offset-3 col-sm-6 text-center">
								<h4>Suggested Terms</h4>
							</div>
						</div>

						<div class="form-group">
							<div class="col-sm-offset-1 col-sm-10">
								<table id="suggestedTermsTable"
									class="table table-bordered table-condensed stripe text-left display"
									cellspacing="0" width="100%">
									<thead>
										<tr>
			<th>Data</th>
			<th>Term ID</th>
			<th>GO Aspect</th>
			<th>Term</th>
			<th>Overlap</th>
			<th>Term Size</th>
										</tr>
									</thead>

									<tbody>
									</tbody>
								</table>
							</div>
						</div>

					</form>
				</div>
				<div id="inspectPool" class="custom-transition">
					
				</div>
			</div>
			<div class="modal-footer"></div>
		</div>
	</div>
</div>
