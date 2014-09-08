<div class="container">
	<form id="overview" class="form-horizontal" role="form">

		<div class="alert alert-warning" id="overviewFailed"
			hidden="true">
			<a href="#" class="close" data-hide="alert">&times;</a>
			<div id="overviewMessage" class="text-center">Some message.</div>
		</div>


<!-- 		<div class="form-group">
			<div class="col-sm-offset-4 col-sm-4">
				<span id=overviewName></span>
				<span id=overviewEmail class="pull-right"></span>
			</div>
		</div> -->

		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6 text-center">
				<h4>Personal Profile</h4>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-1 text-left">
				<span>Name:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewName>Name:</span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-1 text-left">
				<span>Email:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewEmail>Email:</span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-1 text-left">
				<span>Organization:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewOrganisation>Organization:</span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-1 text-left">
				<span>Website:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewURL>Website:</span>
			</div>
		</div>
		
		</br>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6 text-center">
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
			<div class="col-sm-offset-3 col-sm-6 text-center">
				<h4>Model Organisms Studied</h4>
			</div>
		</div>
		
		<div class="alert alert-warning" id="overviewModelFailed"
			hidden="true">
			<a href="#" class="close" data-hide="alert">&times;</a>
			<div id="overviewModalMessage" class="text-center">No genes have been added.</div>
		</div>
		
		<div id="overviewGeneBreakdown" class="form-group">

		</div>

	</form>
</div>

<div class="modal fade" id="scrapModal" tabindex="-1"
    role="dialog" aria-labelledby="scrapModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span><span
                        class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="scrapModalLabel">All Genes</h4>
            </div>
            <div class="modal-body">

			    <form id="scrapModalForm" class="form-horizontal" role="form">
			    
					<div class="alert alert-warning" id="scrapModalFailed"
						hidden="true">
						<a href="#" class="close" data-hide="alert">&times;</a>
						<div id="scrapModalMessage" class="text-center">Something went wrong.</div>
					</div>
			    
			        <div class=" form-group">
						<div class="col-sm-12">
	                        <table id="scrapModalTable" class="table table-condensed">
	                           <thead>
	                              <tr>
	                                 <th>Symbol</th>
	                                 <th>Name</th>
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
					<div class="col-sm-offset-1 col-sm-10">
						<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
					</div>
				</div>
		    </div>
        </div>
    </div>
</div>

<!-- Our scripts -->
<script src="scripts/api/overview.js"></script>