<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Disease Project</title>

<!-- Bootstrap core CSS -->
<link href="styles/bootstrap.min.css" rel="stylesheet">

<!-- Optional Bootstrap Theme -->
<link href="data:text/css;charset=utf-8,"
    data-href="styles/bootstrap-theme.min.css" rel="stylesheet"
    id="bs-theme-stylesheet">

</head>
<body>


    <!-- Modal -->
    <div class="modal bs-modal-sm" id="myModal" tabindex="-1"
        role="dialog" aria-labelledby="mySmallModalLabel"
        aria-hidden="true">
        <div class="modal-dialog" data-backdrop="static"
            data-show="true">
            <div class="modal-content" data-backdrop="static"
                data-show="true">
                <br>
                <div class="bs-example bs-example-tabs">
                    <ul id="myTab" class="nav nav-tabs">
                        <li class="active"><a href="#signin"
                            data-toggle="tab">Sign In</a></li>
                        <li class=""><a href="#signup"
                            data-toggle="tab">Register</a></li>
                        <li class=""><a href="#resetPassword"
                            data-toggle="tab">Reset Password</a></li>
                        <li class=""><a href="#why"
                            data-toggle="tab">Why?</a></li>
                    </ul>
                </div>
                <div class="modal-body">
                    <div id="myTabContent" class="tab-content">
                        <div class="tab-pane fade in" id="why">
                            <p>We need this information so that you
                                can receive access to the site and its
                                content. Rest assured your information
                                will not be sold, traded, or given to
                                anyone.</p>
                            <p></p>
                            <br> Please contact <a
                                mailto:href="rdp@chibi.ubc.ca"></a>rdp@chibi.ubc.ca</a>
                            for any other inquiries.
                            </p>
                        </div>
                        <div class="tab-pane fade active in" id="signin">
                            <form id="signinForm"
                                class="form-horizontal"
                                data-toggle="validator" role="form">
                                <fieldset>
                                    <!-- Sign In Form -->
                                    <div class="alert alert-warning"
                                        id="signinFailed" hidden="true">
                                        <a href="#" class="close"
                                            data-dismiss="alert">&times;</a>
                                        <strong>Warning!</strong> Login
                                        username/password incorrect.
                                    </div>

                                    <!-- Text input-->
                                    <div class="control-group">
                                        <label class="control-label"
                                            for="signinId">Username:</label>
                                        <div class="controls">
                                            <input required
                                                id="signinId"
                                                name="signinId"
                                                type="text"
                                                class="form-control"
                                                placeholder="joe123"
                                                class="input-medium"
                                                required="">
                                        </div>
                                    </div>

                                    <!-- Password input-->
                                    <div class="control-group">
                                        <label class="control-label"
                                            for="signinPassword">Password:</label>
                                        <div class="controls">
                                            <input required
                                                id="signinPassword"
                                                name="signinPassword"
                                                class="form-control"
                                                type="password"
                                                placeholder="********"
                                                class="input-medium">
                                        </div>
                                    </div>

                                    <!-- Button -->
                                    <div class="control-group">
                                        <label class="control-label"
                                            for="signin"></label>
                                        <div class="controls">
                                            <button for="btnSignin"
                                                id="btnSignin"
                                                name="signin"
                                                class="btn btn-success">Sign
                                                In</button>
                                        </div>
                                    </div>
                                </fieldset>
                            </form>
                        </div>
                        
                        <!-- User signup -->
                        <%@ include file="signupForm.jsp" %>
                        
                        <!-- Reset password -->
                        <%@ include file="resetPasswordForm.jsp" %>
                        
                    </div>
                </div>
                <!-- 
      <div class="modal-footer">
      <center>
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </center>
      </div>
       -->

            </div>
        </div>
    </div>

    <!-- include jQuery, jQuery UI, and our script file -->
    <script src="scripts/lib/jquery-1.11.1.min.js"></script>
    <script src="scripts/lib/bootstrap.min.js"></script>
    <script src="scripts/lib/jquery.validate.min.js"></script>
    <script type="text/javascript"
        src="http://www.google.com/recaptcha/api/js/recaptcha_ajax.js"></script>

    <!-- Our scripts -->
    <script src="scripts/api/login.js"></script>
    <script src="scripts/api/signup.js"></script>
    <script src="scripts/api/resetPassword.js"></script>
    <script src="scripts/api/register.js"></script>
    
</body>
</html>
