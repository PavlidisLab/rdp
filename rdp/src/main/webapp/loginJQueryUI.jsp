<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Disease Project Login</title>

<!-- Declare our jQuery UI Theme CSS, this came from the download builder -->
<!-- 
<link rel="stylesheet" href="styles/ui-lightness/ui-lightness.css"
    type="text/css" />
 -->

<!-- Our styles -->
<!--
<link rel="stylesheet" href="styles/layout.css" type="text/css" /> 
 -->

<link rel="stylesheet" href="styles/bootstrap-responsive.css" type="text/css" />

</head>
<body id="login">

    <div id="content">

        <!-- 
                 Our login form container, we define this as the widget container
                 with ui-widget, and make the containers corners rounded 
                 with the ui-corner-all class. 
        -->
        <section class="ui-widget ui-corner-all">

            <!-- 
                     A header for the widget container, defined by ui-widget-header
                     and we are expecting only the top corners to be rounded 
                     with the ui-corner-top class. 
            -->
            <header class="ui-widget-header ui-corner-top">Rare
                Disease Project Login</header>

            <!-- 
                 Define the start of the widget's content with ui-widget-content
                     Make the bottom of the content container corner rounded 
                     with the ui-corner-bottom class. 
            -->
            <div class="ui-widget-content ui-corner-bottom">
                <p>Please use the login form below to login to your
                    to-do list.</p>

                <!-- 
                            The CSS Framework provides a ui-state-error class to help style
                            error messages.  We'll use this class to style an error message 
                            on an incorrect login attempt
                -->
                <p class="ui-state-error">Login username/password
                    incorrect.</p>

                <!-- Our login form -->
                <form>
                    <div>
                        <label for="username">Username:</label> <input
                            type="text" id="username"
                            value="administrator">
                    </div>
                    <div>
                        <label for="password">Password:</label> <input
                            type="password" id="password"
                            value="changemeadmin">
                    </div>
                    <div>
                        <input type="submit" id="btnLogin" value="Login">
                        <button class="btnCreateAccount">Register</button>
                    </div>

                </form>

                <!-- Create registration dialog -->
                <form id="createAccountFrom" title="Create a new account">
                    <div>
                        <label for="username">Username:</label> <input
                            type="text" id="username" value="testuser">
                    </div>
                    <div>
                        <label for="email">Email:</label> <input
                            type="text" id="email"
                            value="testuser@123.com">
                    </div>
                    <div>
                        <label for="confirmEmail">Confirm Email:</label>
                        <input type="text" id="confirmEmail"
                            value="testuser@123.com">
                    </div>
                    <div>
                        <label for="password">Password:</label> <input
                            type="password" id="password"
                            value="changemeadmin">
                    </div>
                    <div>
                        <label for="confirmPassword">Confirm
                            password:</label> <input type="password"
                            id="confirmPassword" value="changemeadmin">
                    </div>
                    <div>
                        <label for="captcha">Prove that you are
                            human:</label> <input type="password" id="captcha">
                    </div>
                </form>
                
            </div>


        </section>

    </div>

    <!-- 
             An element that's used to create a overlay effect for the login widget.
             The framework class ui-widget-overlay defines the style. 
    -->
    <div class="ui-widget-overlay" style="z-index: 1002;"></div>

    <!-- include jQuery, jQuery UI, and our script file -->
    <script src="scripts/lib/jquery-1.11.1.min.js"></script>
    <!-- 
    <script src="scripts/lib/jquery-ui-1.11.0.js"></script>
     -->
    <script src="scripts/lib/bootstrap.min.js"></script>
    
    <!-- 
    <script src="scripts/api/login.js"></script>
     -->

</body>
</html>
