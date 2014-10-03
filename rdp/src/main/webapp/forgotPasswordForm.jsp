<div class="tab-pane fade" id="forgotPassword">
    <form id="forgotPasswordForm" class="form-horizontal" role="form">
            <div class="alert alert-warning col-sm-offset-1 col-sm-10" id="forgotPasswordFailed" hidden="true">
				<a href="#" class="close" data-hide="alert">&times;</a>
				<div id="forgotPasswordMessage" class="text-left"></div>
			</div>
			<div class="form-group">
				<div class="col-sm-offset-1 col-sm-10">
					<span>Password reset instructions will be sent to your registered email address.</span>
				</div>
			</div>
	        <div class="form-group">
	            <label for="forgotPasswordEmail" class="col-sm-3 control-label">Email:</label>
	            <div class="col-sm-8">
	                <input type="email" name="forgotPasswordEmail" class="form-control"
	                    id="forgotPasswordEmail" placeholder="joe@rdp.com" required>
	            </div>
	        </div>

			<div class="form-group">
				<div class="col-sm-offset-3 col-sm-8 text-right">
					<button type="submit" id="btnForgotPasssword" class="btn btn-success">Submit</button>
				</div>
			</div>
    </form>
</div>