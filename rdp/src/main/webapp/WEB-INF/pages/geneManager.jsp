<!-- Display list of genes in a modal -->
<div class="modal bs-modal-sm" id="geneManagerModal" role="dialog"
	aria-labelledby="genesModalLabel" aria-hidden="true">
	<div class="modal-dialog" data-backdrop="static" data-show="true">
		<div class="modal-content" data-backdrop="static" data-show="true">

			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">
					<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
				</button>
				<h4 class="modal-title">My genes</h4>
			</div>

			<div class="modal-body">
				<%@ include file="searchGenes.jsp"%><br />
				<%@ include file="geneManagerTable.jsp"%>
			</div>

			<div class="modal-footer">
				<button type="button" id="closeGenesButton" class="btn btn-default"
					data-dismiss="modal">Close</button>
				<button type="button" id="saveGenesButton" class="btn btn-primary">Save</button>
			</div>

		</div>
	</div>
</div>
