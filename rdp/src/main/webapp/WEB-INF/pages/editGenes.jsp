<!-- Modal -->
<div class="modal fade bs-example-modal-lg" id="editGenesModal"
    role="dialog" aria-labelledby="myModalGenesLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span><span
                        class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="myModalGenesLabel">Model Organism Profile</h4>
            </div>
            <div class="modal-body">
				<form id="modelOrganism" class="form-horizontal" role="form">
			
					<div class="alert alert-warning" id="modelOrganismFailed"
						hidden="true">
						<a href="#" class="close" data-dismiss="alert">&times;</a>
						<div id="modelOrganismMessage">Failed to save model organism
							details.</div>
					</div>
			
					<div class="form-group">
						<label class="col-sm-3 control-label">Organism</label>
						<div class="col-sm-6">
							<select id="taxonCommonNameSelect" class="form-control">
								<option>Human</option>
								<option>Mouse</option>
								<option>Rat</option>
								<!-- FIXME
								<option>Zebrafish</option>
			                    <option>Fruitfly</option>
			                    <option>Worm</option>
			                    <option>Yeast</option>
			                    <option>E-coli</option>
							     -->
							</select>
						</div>
					</div>
			
					<div class="form-group">
						<label class="col-sm-3 control-label">About</label>
						<div class="col-sm-6">
							<textarea class="form-control" rows="3"
								placeholder="My lab studies ..."></textarea>
						</div>
					</div>
			
<!-- 					<div class="form-group">
						<label class="col-sm-3 control-label">Genes</label> 
						<label> 
						BRCA1, APOE, SNCA, ...
			            
						</label>
			
						
			
					</div> -->
			
					<!-- This can be cumbersome to edit when there's a lot of genes
			        <div class="form-group">
			                    <label class="col-sm-3 control-label">Genes</label>
			                    <div class="col-sm-6">
			                        <select id="geneSelect" class="select2"
			                            multiple="multiple" style="width: 100%;">
			                            <option>BRCA1</option>
			                            <option>APOE</option>
			                            <option>SNCA</option>
			                            <option>CAMK2A</option>
			                        </select>
			                    </div>
			                </div>
			         -->
			
			
<!-- 					<button id="submit" type="submit"
						class="btn btn-default col-sm-offset-3">Save</button> -->
					<br />
					<div class="form-group">
						<div class="col-sm-offset-1 col-sm-10">
							<%@ include file="searchGenes.jsp"%>
						</div>
					</div>
					<br />
					<div class="form-group">
						<div class="col-sm-offset-1 col-sm-10">
							<%@ include file="geneManagerTable.jsp"%>
						</div>
					</div>
					
					</br>
					
					<div class="form-group">
						<div class="col-sm-offset-2 col-sm-8">
							<button type="button" id="closeGenesButton" class="btn btn-default"
								data-dismiss="modal">Close</button>
							<button type="button" id="saveGenesButton" class="btn btn-primary">Save</button>
						</div>		
					</div>
				</form>

            </div>
        </div>
    </div>
</div>

<!-- Our scripts -->
