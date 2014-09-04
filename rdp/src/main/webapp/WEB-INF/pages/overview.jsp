<div class="container">
	<form id="overview" class="form-horizontal" role="form">

		<div class="alert alert-warning" id="overviewFailed"
			hidden="true">
			<a href="#" class="close" data-hide="alert">&times;</a>
			<div id="overviewMessage">Some message.</div>
		</div>


<!-- 		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4">
				<span id=overviewName></span>
				<span id=overviewEmail class="pull-right"></span>
			</div>
		</div> -->
		
		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4 text-center">
				<span id=overviewName></span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4 text-center">
				<span id=overviewEmail></span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4 text-center">
				<span id=overviewOrganisation></span>
			</div>
		</div>
		
		</br>
		
		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4 text-center">
				<h4>Research Focus</h4>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6">
				<textarea class="form-control" rows="3"
					placeholder="My lab studies ..."></textarea>
			</div>
		</div>
		
		</br>
		
		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4 text-center">
				<h4>Model Organisms Studied</h4>
			</div>
		</div>
		
		<div id="overviewGeneBreakdown" class="form-group">

		</div>

	</form>
</div>


<!-- Our scripts -->
<script src="scripts/api/overview.js"></script>