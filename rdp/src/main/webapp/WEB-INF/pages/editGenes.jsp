<!-- Modal -->
<div class="modal fade bs-example-modal-lg" id="editGenesModal"
	role="dialog" aria-labelledby="myModalGenesLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="btn-lg close" data-dismiss="modal" style="font-size:30px">
					<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
				</button>
				<h4 class="modal-title" id="myModalGenesLabel">Model Organism
					Profile</h4>
									
			</div>
			
			<div class="modal-body">
				
				<form id="modelOrganism" class="form-horizontal" role="form">

					<div class="form-group">
						<div class="col-sm-offset-2 col-sm-9 text-right">
							<button type="button" class="btn btn-primary saveGenesButton">Save</button>
<!-- 							<button type="button" id="closeGenesButton"
								class="btn btn-default" data-dismiss="modal">Close</button> -->
						</div>
					</div>

					<div class="form-group">
						<label class="col-sm-2 control-label">Organism</label>
						<div class="col-sm-9">
							<select id="taxonCommonNameSelect" class="form-control">
								<option>Human</option>
								<option>Mouse</option>
								<option>Rat</option>
								<option>Yeast</option>
								<option>Zebrafish</option>
			                    <option>Fruitfly</option>
			                    <option>Worm</option>
			                    <option>E. Coli</option>
							</select>
						</div>
					</div>

					<div class="form-group">
						<label class="col-sm-2 control-label">Research Focus</label>
						<div class="col-sm-9">
							<textarea class="form-control" rows="3"
								placeholder="My lab studies ..."></textarea>
						</div>
						
					</div>
					<div class="boxedin">
					<%@ include file="searchGenes.jsp"%>

					
					<%@ include file="importGenes.jsp"%>
					</div>
					
					<div class="alert alert-warning col-sm-offset-1 col-sm-10" id="geneManagerFailed" hidden="true">
						<a href="#" class="close" data-hide="alert">&times;</a>
						<div id="geneManagerMessage" class="text-left">Failed to load genes.</div>
					</div>
	
					<%@ include file="geneManagerTable.jsp"%>
					
				</form>
				
			</div>
			<div class="modal-footer">
			
				<div class="form-group">
					<div class="col-sm-offset-1 col-sm-10" style="padding-top:20px">
						<button type="button" class="btn btn-primary saveGenesButton">Save</button>
<!-- 						<button type="button" id="closeGenesButton"
							class="btn btn-default" data-dismiss="modal">Close</button> -->
					</div>
				</div>
			</div>

		</div>
	</div>
</div>

<!-- Our scripts -->
