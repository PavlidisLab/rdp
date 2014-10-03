<!-- User signup -->
<div class="tab-pane fade" id="signup">
    <form id="signupForm" class="form-horizontal" role="form">
            <!-- Sign Up Form -->
	        <div class="alert alert-warning col-sm-offset-1 col-sm-10" id="signupFailed" hidden="true">
				<a href="#" class="close" data-hide="alert">&times;</a>
				<div id="signupMessage" class="text-left">Registration failed.</div>
			</div>
	        
	        <div class="form-group">
	            <div class="col-sm-offset-3 col-sm-6">
	        		<span><strong>Email</strong></span>
	                <input type="email" name="email" class="form-control"
	                    id="email" placeholder="joe@rdp.com" required>
	            </div>
	        </div>
	        
	        <div class="form-group">
	            <div class="col-sm-offset-3 col-sm-6">
	            	<span><strong>Confirm Email</strong></span>
	                <input type="email" name="emailConfirm" class="form-control"
	                    id="emailConfirm" placeholder="joe@rdp.com" required>
	            </div>
	        </div>
	        
	        <div class="form-group">
	            <div class="col-sm-offset-3 col-sm-6">
	            	<span><strong>Password</strong></span>
	                <input type="password" name="password" class="form-control"
	                    id="password" placeholder="********" required>
	            </div>
	        </div>
	        
	        <div class="form-group">
	            <div class="col-sm-offset-3 col-sm-6">
	            	<span><strong>Confirm Password</strong></span>
	                <input type="password" name="passwordConfirm" class="form-control"
	                    id="passwordConfirm" placeholder="********" required>
	            </div>
	        </div>

            <%@ page import="ubc.pavlab.rdp.server.util.Settings"%>
            <div class="form-group">
                <label class="recaptcha_only_if_image control-label col-sm-offset-2">Please enter the text below:</label>
                <div class="col-sm-offset-2" id="captchadiv"></div>
                <div id="captchaPublicKey" hidden="true"><%=Settings.getProperty( "rdp.recaptcha.publicKey" )%></div>
            </div>
			
			<div class="form-group">
				<div class="col-sm-offset-3 col-sm-6 text-right">
					<button type="submit" id="btnSignup" class="btn btn-success">Register</button>
				</div>
			</div>

    </form>
</div>