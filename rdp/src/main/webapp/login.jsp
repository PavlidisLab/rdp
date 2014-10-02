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

<style>
	/*Lighten placeholder text*/
	.form-control::-moz-placeholder {
	  color: #A9A9A9;
	  opacity: 1;
	}
	.form-control:-ms-input-placeholder {
	  color: #A9A9A9;
	}
	.form-control::-webkit-input-placeholder {
	  color: #A9A9A9;
	}
</style>

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
                        <li class=""><a href="#forgotPassword"
                            data-toggle="tab">Forgot Password</a></li>
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
                            <br> Please contact <a href="mailto:rdp@chibi.ubc.ca">rdp@chibi.ubc.ca</a>
                            for any other inquiries.
                            </p>
                        </div>
                        <div class="tab-pane fade active in" id="signin">
                            <form id="signinForm"
                                class="form-horizontal" role="form">
                                    <!-- Sign In Form -->
                  					<div class="alert alert-warning col-sm-offset-1 col-sm-10" id="signinFailed" hidden="true">
										<a href="#" class="close" data-hide="alert">&times;</a>
										<div id="signinMessage" class="text-left"><strong>Warning!</strong> Login
                                        email/password incorrect.</div>
									</div>

							        <div class="form-group">
							            <label for="email" class="col-sm-3 control-label">Email:</label>
							            <div class="col-sm-8">
							                <input type="text" name="email" class="form-control"
							                    id="email" placeholder="joe@rdp.com" required>
							            </div>
							        </div>
							        
							        <div class="form-group">
							            <label for="password" class="col-sm-3 control-label">Password:</label>
							            <div class="col-sm-8">
							                <input type="password" name="password" class="form-control"
							                    id="password" placeholder="********" required>
							            </div>
							        </div>

									<div class="form-group">
										<div class="col-sm-offset-3 col-sm-8 text-right">
											<button type="submit" id="btnSignin" class="btn btn-success">Sign In</button>
										</div>
									</div>
                            </form>
                        </div>
                        
                        <!-- User signup -->
                        <%@ include file="signupForm.jsp" %>
                        
                        <!-- Reset password -->
                        <%@ include file="forgotPasswordForm.jsp" %>
                        
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
    <script src="scripts/lib/jquery-1.11.1.js"></script>
    <script src="scripts/lib/bootstrap.min.js"></script>
    <script src="scripts/lib/jquery.validate.min.js"></script>
    <script type="text/javascript"
        src="http://www.google.com/recaptcha/api/js/recaptcha_ajax.js"></script>

    <!-- Our scripts -->
    <script src="scripts/api/utility.js"></script>
    <script src="scripts/api/login.js"></script>
    <script src="scripts/api/signup.js"></script>
    <script src="scripts/api/forgotPasswordForm.js"></script>
    
</body>
</html>
