<!-- Modal -->
<div class="modal fade" id="changePasswordModal" tabindex="-1"
    role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span><span
                        class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="myModalLabel">Change
                    Password</h4>
            </div>
            <div class="modal-body">

                <form id="changePasswordForm" class="form-horizontal"
                    data-toggle="validator" role="form">
                    <fieldset>
                        <!-- Change Password Form -->
                        <div class="alert alert-warning"
                            id="changePasswordFailed" hidden="true">
                            <a href="#" class="close"
                                data-hide="alert">&times;</a>
                            <div id="changePasswordMesssage">Edit
                                failed.</div>
                        </div>

                        <!-- Text input-->
                        <div class="control-group" hidden="true">
                            <label class="control-label" for=username>Username:</label>
                            <div class="controls">
                                <input id="username" name="username"
                                    class="form-control" type="text"
                                    placeholder="joe123"
                                    class="input-large" required="false">
                            </div>
                        </div>
                        
                        <!-- Text input-->
                        <div class="control-group">
                            <label class="control-label" for="email">Email:</label>
                            <div class="controls">
                                <input id="email" name="email"
                                    class="form-control" type="email"
                                    placeholder="joe@rdp.com"
                                    class="input-large" required>
                            </div>
                        </div>

                        <!-- Password input-->
                        <div class="control-group">
                            <label class="control-label"
                                for="oldpassword">Current
                                password:</label>
                            <div class="controls">
                                <input
                                    id="oldpassword"
                                    name="oldpassword"
                                    class="form-control" type="password"
                                    placeholder="********"
                                    class="input-large" required="true"
                                    minLength="6">
                            </div>
                        </div>

                        <!-- Password input-->
                        <div class="control-group">
                            <label class="control-label" for="password">New
                                password:</label>
                            <div class="controls">
                                <input id="password" name="password"
                                    class="form-control" type="password"
                                    placeholder="********"
                                    class="input-large" required="true"
                                    minLength="6">
                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="control-group">
                            <label class="control-label"
                                for="passwordConfirm">Confirm
                                new password:</label>
                            <div class="controls">
                                <input id="passwordConfirm"
                                    class="form-control"
                                    name="passwordConfirm"
                                    type="password"
                                    placeholder="********"
                                    class="input-large"
                                    equalTo="#password" required
                                    minLength="6">
                            </div>
                        </div>

                        <!-- Button -->
                        <div class="col-sm-offset-1 col-sm-10 control-group">
                            <label class="control-label"
                                for="confirmsignup"></label>
                            <div class="controls">
                                <button for="btnChangePassword"
                                    id="btnChangePassword"
                                    name="btnChangePassword"
                                    class="btn btn-primary">Save</button>
                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
    </div>
</div>

<!-- include jQuery, jQuery UI, and our script file -->
<script src="scripts/lib/jquery-1.11.1.min.js"></script>
<script src="scripts/lib/bootstrap.min.js"></script>
<script src="scripts/lib/jquery.validate.min.js"></script>

<!-- Our scripts -->
<script src="scripts/api/editUser.js"></script>