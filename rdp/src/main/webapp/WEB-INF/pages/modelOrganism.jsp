<div class="container">
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
					<!-- FIXME
                    <option>Mouse</option>
                    <option>Rat</option>
                    <option>Fly</option>
                    <option>Worm</option>
                    <option>Monkey</option>
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

		<div class="form-group">
			<label class="col-sm-3 control-label">Genes</label> <label
				class="col-sm-5">BRCA1, APOE, SNCA, ...</label>
			<button type="button" id="geneManagerButton" name="geneManagerButton"
				class="btn btn-default btn-sm">
				<span class="glyphicon glyphicon-pencil"></span>
			</button>

			<%@ include file="geneManager.jsp"%>

		</div>

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


		<button id="submit" type="submit"
			class="btn btn-default col-sm-offset-3">Save</button>
	</form>
</div>


<!-- Our scripts -->
<script src="scripts/api/modelOrganism.js"></script>
