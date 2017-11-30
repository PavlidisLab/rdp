<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>${ SETTINGS["rdp.fullname"]}</title>

<!--[if lte IE 9]>
<link href="styles/ie-compat.css" rel="stylesheet">
<![endif]-->

<![if !(lte IE 9)]>
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
 <![endif]>
</head>

<!--[if lte IE 9]>
	<body>
		<div class="ie-compat">
			<div class="ie-compat-header">
				<h1> <a href="http://www.rare-diseases-catalyst-network.ca/">${ SETTINGS["rdp.fullname"]}</a></h1>
			</div>
			<div>
			<p>Sorry, this application does not support Internet Explorer versions before 10.0. Please try again with a more modern browser.
			<img src="images/browser.png"/>
			</div>
			</p>
			<div class="push"></div>
		</div>
		<div class="ie-compat-footer">
			� Copyright 2014. "${ SETTINGS["rdp.fullname"]}" All rights reserved. <a href="mailto:registry-help@rare-diseases-catalyst-network.ca" style="font-size:15px">Contact Support</a>
		</div>

<![endif]-->

	</body>


<![if !(lte IE 9)]>
<body>



    <!-- Modal -->
    <div class="modal bs-modal-sm" id="myModal" tabindex="-1"
        role="dialog" aria-labelledby="mySmallModalLabel"
        aria-hidden="true">
        <div class="modal-dialog" data-backdrop="static"
            data-show="true">
            <div class="modal-content" data-backdrop="static" data-show="true">
                <div class="modal-header">
                
					<h3 class="modal-title text-center"><a href="#signin" data-toggle="tab">${ SETTINGS["rdp.fullname"]}</a></h3>
					<h5 class="modal-title text-center">Researcher Registry</h5>
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
						<a href="mailto:${ SETTINGS["rdp.contact.email"]}" style="font-size:15px">Contact Support</a>
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
        src="//www.google.com/recaptcha/api/js/recaptcha_ajax.js"></script>
	<script src="scripts/lib/modernizr.custom.31926.js"></script>
    <!-- Our scripts -->
    <script src="scripts/api/utility.js"></script>
    <script src="scripts/api/login.js"></script>
    <script src="scripts/api/signup.js"></script>
    <script src="scripts/api/forgotPasswordForm.js"></script>
    <script> 
    $(document).ready(function() {
    	if(!Modernizr.input.placeholder){
    		$("input").each(
    			function(){
    				if($(this).val()=="" && $(this).attr("placeholder")!=""){
    					$(this).val($(this).attr("placeholder"));
    					$(this).focus(function(){
    						if($(this).val()==$(this).attr("placeholder")) $(this).val("");
    					});
    					$(this).blur(function(){
    						if($(this).val()=="") $(this).val($(this).attr("placeholder"));
    					});
    				}
    		});
    	}
    });
    </script>
    
</body>
 <![endif]>
</html>
