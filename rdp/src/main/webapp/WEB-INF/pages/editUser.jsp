<!-- Modal -->
<div class="modal fade" id="changePasswordModal" tabindex="-1"
    role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" style="font-size:30px">
                    <span aria-hidden="true">&times;</span><span
                        class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="myModalLabel">Change
                    Password</h4>
            </div>
            <div class="modal-body">

                <form id="changePasswordForm" class="form-horizontal" role="form">
                        <!-- Change Password Form -->                     
                    <div class="form-group">
			            <div class="alert alert-warning col-sm-offset-1 col-sm-10"
			                id="changePasswordFailed" hidden="true">
			                <a href="#" class="close" data-hide="alert">&times;</a>
			                <div id="changePasswordMessage">Failed saving
			                    details.</div>
			            </div>
			        </div>
                        
			        <div class="form-group">
			            <label for="oldPassword" class="col-sm-4 control-label">Current Password</label>
			            <div class="col-sm-7">
			                <input type="password" name="oldPassword" class="form-control"
			                    id="oldPassword" placeholder="********">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label for="password" class="col-sm-4 control-label">New Password</label>
			            <div class="col-sm-7">
			                <input type="password" name="password" class="form-control"
			                    id="password" placeholder="********">
			            </div>
			        </div>
			
			        <div class="form-group">
			            <label for="passwordConfirm" class="col-sm-4 control-label">Confirm Password</label>
			            <div class="col-sm-7">
			                <input type="password" name="passwordConfirm" class="form-control"
			                    id="passwordConfirm" placeholder="********">
			            </div>
			        </div>
                        
                    <hr>
                    
					<div class="form-group">
						<div class="col-sm-offset-4 col-sm-7 text-right">
							<button type="submit" id="btnChangePassword" class="btn btn-primary">Save</button>
						</div>
					</div>
                        
                </form>
            </div>
        </div>
    </div>
</div>

<!-- include jQuery, jQuery UI, and our script file -->
<!-- Our scripts -->
