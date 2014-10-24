<div class="container">
	<form id="overviewForm" class="form-horizontal" role="form">

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
		
		<div class="shadowboxedin">
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6 text-center">
				<h3>Personal Profile</h3>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-2 text-left">
				<span>Name:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewName></span>
			</div>
		</div>
		
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-2 text-left">
				<span>Email:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewEmail></span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-2 text-left">
				<span>Organization:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewOrganisation></span>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-2 text-left">
				<span>Website:</span>
			</div>
			<div class="col-sm-4 text-left">
				<span id=overviewURL></span>
			</div>
		</div>
		
		</br>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6 text-center">
				<h4>Research Interests</h4>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6">
				<p id="overviewFocus" class="form-control-static"></p>
			</div>
		</div>
		
		<div class="form-group">
			<div class="col-sm-offset-8 col-sm-4">
	            <a id="overviewEditDescriptionButton"
	               class="btn btn-default btn-xs" role="button" data-toggle="modal"
	               data-placement="bottom" title="Edit Personal Profile" href="#editProfileModal">
	               <span>Edit</span>
	            </a>
            </div>
        </div>
        
		</div>
		
		</br>
		
		
		<div class="form-group">
			<div class="col-sm-offset-3 col-sm-6 text-center">
				<h3>Model Organisms Studied</h3>
			</div>
		</div>
		<div class="form-group">
			<div class="col-sm-offset-5 col-sm-2 text-center">
				<ul id="overviewTab" class="nav nav-tabs" role="tablist">
		
					<li class="active"><a href="#geneOverviewTab" role="tab" data-toggle="tab">Genes</a></li>
		
					<li><a href="#termOverviewTab" role="tab" data-toggle="tab"">Terms</a></li>
		
				</ul>
			</div>
		</div>
		<div class="tab-content">
        	<div class="tab-pane fade in active" id="geneOverviewTab">
				<div class="alert alert-warning" id="overviewModelFailed"
					hidden="true">
					<a href="#" class="close" data-hide="alert">&times;</a>
					<div id="overviewModelMessage" class="text-center">No genes have been added.</div>
				</div>
				
				<div id="overviewGeneBreakdown" class="form-group">
		
				</div>
			</div>
        	<div class="tab-pane fade in active" id="termOverviewTab">
				<div class="alert alert-warning" id="overviewTermModelFailed"
					hidden="true">
					<a href="#" class="close" data-hide="alert">&times;</a>
					<div id="overviewTermModelMessage" class="text-center">No GO terms have been added.</div>
				</div>
				
				<div id="overviewTermBreakdown" class="form-group">
		
				</div>
			</div>
		</div>
		


	</form>
</div>

<div class="modal fade" id="scrapModal" tabindex="-1"
    role="dialog" aria-labelledby="scrapModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" style="font-size:30px">
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
			    
      

			    </form>

            </div>
		    <!--<div class="modal-footer">
 				<div class="form-group">
					<div class="col-sm-offset-1 col-sm-10">
						<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
					</div>
				</div> 
		    </div>-->
        </div>
    </div>
</div>

<!-- Our scripts -->
