<!-- 

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 -->




<div class="row">
	<!-- FIXME Get taxon from current form -->
	<div class="col-sm-1">Symbol</div>
	<input type="hidden" class="bigdrop col-sm-10" id="searchGenesSelect" />
	<!-- 
	<select id="searchGenesSelect" class="col-sm-10">
	   <option value="AL">Alabama</option>
        <option value="WY">Wyoming</option>
	 
	</select>
	-->
	<button type="button" class="btn btn-default">
		<span class="glyphicon glyphicon-plus-sign"></span>
	</button>
</div>

<!-- Our scripts -->
<script src="scripts/api/searchGenes.js"></script>