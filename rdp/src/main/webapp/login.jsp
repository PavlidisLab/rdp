<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Diseases: Models & Mechanisms Network</title>

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
	
	.shadowboxedin { 
		padding: 10px 0;
		margin-bottom: 10px;
		-webkit-box-shadow: 0px 2px 2px rgba(0, 0, 0, 0.3);
		box-shadow: 0px 2px 2px rgba(0, 0, 0, 0.3);
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
            <div class="modal-content" data-backdrop="static" data-show="true">
                <div class="modal-header">
					<h3 class="modal-title text-center"><a href="#signin" data-toggle="tab">Rare Diseases</a></h3>
					<h5 class="modal-title text-center">Models & Mechanisms Network</h5>
	            </div>
                <div class="modal-body">
                	<div class="tab-content">
                		<div class="tab-pane fade in active" id="signin">
		                    <form id="signinForm" class="form-horizontal shadowboxedin" role="form">
		                    <!-- Sign In Form -->
			              		<div class="alert alert-warning col-sm-offset-1 col-sm-10" id="signinFailed" hidden="true">
									<a href="#" class="close" data-hide="alert">&times;</a>
									<div id="signinMessage" class="text-left"><strong>Warning!</strong> Login
			                                    email/password incorrect.</div>
								</div>
			
						        <div class="form-group">
						            <div class="col-sm-offset-3 col-sm-6">
						                <input type="text" name="email" class="form-control"
						                    id="email" placeholder="Email" required>
						            </div>
						        </div>
						        
						        <div class="form-group">
						            <div class="col-sm-offset-3 col-sm-6">
						                <input type="password" name="password" class="form-control"
						                    id="password" placeholder="Password" required>
						            </div>
						        </div>
			
								<div class="form-group">
									<div class="col-sm-offset-3 col-sm-4 text-left">
										<a href="#forgotPassword" data-toggle="tab" style="font-size:12px">Forgot password?</a>
									</div>
									<div class="col-sm-2 text-right">
										<button type="submit" id="btnSignin" class="btn btn-success">Sign in</button>
									</div>
								</div>
		                    </form>  
		                    <div class="row">
			                    <div class="col-sm-offset-3 col-sm-6 text-center">
									<a href="#signup" data-toggle="tab" style="font-size:15px">Create an account</a>
								</div>  
							</div> 
						</div>
						
						<%@ include file="signupForm.jsp" %>
						
						<%@ include file="forgotPasswordForm.jsp" %>
						
					</div>	                   
                </div>

			    <div class="modal-footer">
					<div class="col-sm-offset-9 col-sm-3 text-right">
						<a href="mailto:rdp@chibi.ubc.ca" style="font-size:15px">Contact Support</a>
					</div>
			    </div>


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
