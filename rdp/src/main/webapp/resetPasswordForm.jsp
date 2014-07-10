<div class="tab-pane fade" id="resetPassword">
    <form id="resetPasswordForm" class="form-horizontal"
        data-toggle="validator" role="form">
        <fieldset>
            <!-- Sign In Form -->
            <div class="alert alert-warning" id="resetPasswordFailed" hidden="true">
                <a href="#" class="close" data-dismiss="alert">&times;</a>
                <div id="resetPasswordMessage">Login username/email
                incorrect.</div>
            </div>

            <!-- Text input-->
            <div class="control-group">
                <label class="control-label" for="resetPasswordId">Username:</label>
                <div class="controls">
                    <input required id="resetPasswordId" name="resetPasswordId"
                        type="text" class="form-control"
                        placeholder="joe123" class="input-medium"
                        required="">
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="resetPasswordEmail">Email:</label>
                <div class="controls">
                    <input id="resetPasswordEmail" name="resetPasswordEmail" class="form-control"
                        type="email" placeholder="joe@rdp.com"
                        class="input-large" required>
                </div>
            </div>

            <!-- Button -->
            <div class="control-group">
                <label class="control-label" for="btnResetPasssword"></label>
                <div class="controls">
                    <button for="btnResetPasssword" id="btnResetPasssword" name="btnResetPasssword"
                        class="btn btn-success">Submit</button>
                </div>
            </div>
        </fieldset>
    </form>
</div>