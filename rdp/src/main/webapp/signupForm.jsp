<!-- User signup -->
<div class="tab-pane fade" id="signup">
    <form id="signupForm" class="form-horizontal"
        data-toggle="validator" role="form">
        <fieldset>
            <!-- Sign Up Form -->
            <div class="alert alert-warning" id="signupFailed" hidden="true">
                <a href="#" class="close" data-dismiss="alert">&times;</a>
                <div id="signupMesssage">Registration failed.</div>
            </div>

            <!-- Text input-->
            <div class="control-group">
                <label class="control-label" for="userid">Username:</label>
                <div class="controls">
                    <input id="username" name="username"
                        class="form-control" type="text"
                        placeholder="joe123" class="input-large"
                        required="true">
                </div>
            </div>

            <!-- Password input-->
            <div class="control-group">
                <label class="control-label" for="password">Password:</label>
                <div class="controls">
                    <input id="password" name="password"
                        class="form-control" type="password"
                        placeholder="********" class="input-large"
                        required="true" minLength="6">
                </div>
            </div>

            <!-- Text input-->
            <div class="control-group">
                <label class="control-label" for="passwordConfirm">Re-enter
                    Password:</label>
                <div class="controls">
                    <input id="passwordConfirm" class="form-control"
                        name="passwordConfirm" type="password"
                        placeholder="********" class="input-large"
                        equalTo="#password" required minLength="6">
                </div>
            </div>

            <!-- Text input-->
            <div class="control-group">
                <label class="control-label" for="email">Email:</label>
                <div class="controls">
                    <input id="email" name="email" class="form-control"
                        type="email" placeholder="joe@rdp.com"
                        class="input-large" required>
                </div>
            </div>

            <!-- Text input-->
            <div class="control-group">
                <label class="control-label" for="emailConfirm">Confirm
                    Email:</label>
                <div class="controls">
                    <input id="emailConfirm" name="emailConfirm"
                        class="form-control" type="email"
                        equalTo="#email" placeholder="joe@rdp.com"
                        class="input-large" required>
                </div>
            </div>

            <!-- reCAPTCHA -->
            <%@ page import="ubc.pavlab.rdp.server.util.Settings"%>
            <div class="control-group">
                <label class="recaptcha_only_if_image control-label">Please enter the text below:</label>
                <div class="controls">
                    <div id="captchadiv"></div>
                    <div id="captchaPublicKey" hidden="true"><%=Settings.getProperty( "rdp.recaptcha.publicKey" )%></div>
                </div>
            </div>

            <!-- Button -->
            <div class="control-group">
                <label class="control-label" for="confirmsignup"></label>
                <div class="controls">
                    <button for="btnSignup" id="btnSignup"
                        name="btnSignup" class="btn btn-success">Sign
                        Up</button>
                </div>
            </div>
        </fieldset>
    </form>
</div>