<!-- 

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 -->

<div class="row">

	<input type="hidden" class="bigdrop col-sm-10" id="searchGenesSelect" />

	<button type="button" id="addGeneButton" class="btn btn-default btn-sm"
		data-toggle="tooltip" data-placement="bottom" title="Add gene">
		<span class="glyphicon glyphicon-plus-sign"></span>
	</button>

</div>



<!-- Our scripts -->
<script src="scripts/api/searchGenes.js"></script>