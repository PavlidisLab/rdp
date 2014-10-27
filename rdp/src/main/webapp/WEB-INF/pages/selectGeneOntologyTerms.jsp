<!-- Modal -->
<div class="modal fade bs-example-modal-lg" id="selectGeneOntologyTermsModal"
	role="dialog" aria-labelledby="myModalGenesLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="btn-lg close" data-dismiss="modal" style="font-size:30px">
					<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
				</button>
				<h4 class="modal-title" id="selectGeneOntologyTermsTitle">Select GO Terms</h4>
									
			</div>
			
			<div class="modal-body">
				
				<form id="selectGeneOntologyTermsForm" class="form-horizontal" role="form">
					<div class="form-group">
						<div class="alert alert-warning col-sm-offset-1 col-sm-10" id="selectGeneOntologyTermsFailed" hidden="true">
							<a href="#" class="close" data-hide="alert">&times;</a>
							<div id="selectGeneOntologyTermsMessage" class="text-left"></div>
						</div>
					</div>
	
					<div class="form-group">
						<label class="col-sm-offset-1 col-sm-2 control-label">Add GO Term</label>
						<div class="col-sm-7">
							
								<input type="hidden" class="bigdrop form-control" id="selectGeneOntologyTermsSelect" />					
					
						</div>
								<button type="button" id="selectGeneOntologyTermsAddTermButton" class="btn btn-default" data-toggle="tooltip" data-placement="bottom" title="Add gene">
									<span>Add</span>
								</button>	
					</div>
	
					<div class="form-group">
						<div class="col-sm-offset-3 col-sm-6 text-center">
							<h4>Suggested terms</h4>
						</div>
					</div>
	
					<div class="form-group">
						<div class="col-sm-offset-1 col-sm-10">
							<table id="selectGeneOntologyTermsTable" class="table table-bordered table-condensed stripe text-left display"
								cellspacing="0" width="100%">
								<thead>
									<tr>
										<th>Data</th>
										<th>GO ID</th>
										<th>Aspect</th>
										<th>Term</th>
										<th>Frequency</th>
										<th>Size</th>
										<th>Add</th>
									</tr>
								</thead>
						
								<tbody>
								</tbody>
							</table>
						</div>
					</div>
					
				</form>
				
			</div>
			<div class="modal-footer">
			
				<div class="form-group">
					<div class="col-sm-offset-1 col-sm-10" style="padding-top:20px">
						<button type="button" id="selectGeneOntologyTermsButton" class="btn btn-primary">Save</button>
					</div>
				</div>
			</div>

		</div>
	</div>
</div>

<!-- Our scripts -->
