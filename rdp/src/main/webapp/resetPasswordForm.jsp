<div class="tab-pane fade" id="resetPassword">
    <form id="resetPasswordForm" class="form-horizontal" role="form">
            <div class="alert alert-warning col-sm-offset-1 col-sm-10" id="resetPasswordFailed" hidden="true">
				<a href="#" class="close" data-hide="alert">&times;</a>
				<div id="resetPasswordMessage" class="text-left">Login username/email
                incorrect.</div>
			</div>
			<div class="form-group">
				<div class="col-sm-offset-1 col-sm-10">
					<span>Password reset instructions will be sent to your registered email address.</span>
				</div>
			</div>
	        <div class="form-group">
	            <label for="resetPasswordEmail" class="col-sm-3 control-label">Email:</label>
	            <div class="col-sm-8">
	                <input type="email" name="resetPasswordEmail" class="form-control"
	                    id="resetPasswordEmail" placeholder="joe@rdp.com" required>
	            </div>
	        </div>

			<div class="form-group">
				<div class="col-sm-offset-3 col-sm-8 text-right">
					<button type="submit" id="btnResetPasssword" class="btn btn-success">Reset</button>
				</div>
			</div>
    </form>
</div>