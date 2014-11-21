/**
 * @memberOf login
 */
(function( login, $, undefined ) {

   login.authenticate = function(e) {
      e.preventDefault();
      var form = $( "#signinForm" );
      var email = form.find( "#email" ).val();
      var password = form.find( "#password" ).val();
      // Remember me http://www.baeldung.com/spring-security-remember-me
      // _spring_security_remember_me: rememberMe

      // var rememberMe = form.rememberMe.value;
      $.ajax( {

         cache : false,
         type : 'POST',
         url : "j_spring_security_check",
         async : false,
         data : {
            j_username : email,
            j_password : password,
            ajaxLoginTrue : true
         },
         beforeSend : function(xhr) {
            xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
         },
         success : function(response, xhr) {
            // in case response is malformed JSON
            var success = false;
            try {
               success = jQuery.parseJSON( response ).success;
            } catch(err) {
               success = response.indexOf("success:true") > -1;
            }
            if ( success ) {
               utility.hideMessage($("#signinMessage"));
               document.location = "register.html";
            } else {
               // If the login credentials are not correct,
               // show our error state element.
               var msg;
               try {
                  msg = jQuery.parseJSON( response ).message;
               } catch(err) {
                  msg = "Login Failed."
               }
               utility.showMessage( msg, $("#signinMessage"));
            }
         },
         error : function(response, xhr) {
            console.log( "Error with request. Status is: " + response.status );
            //FIXME This should be removed after all users have reset
            if ( response.responseText.indexOf("Encoded password does not look like BCrypt") > -1) {
               utility.showMessage( 'Please <a href="#forgotPassword" data-toggle="tab"><strong>reset your password</strong></a>, sorry for the inconvenience.', $("#signinMessage") );
            } else {
               utility.showMessage( "Error with request. Status is: " + response.status, $("#signinMessage") );
            }
         }
      } );
      
      return false;
   };


}( window.login = window.login || {}, jQuery ));

var urlParams;
(window.onpopstate = function () {
    var match,
        pl     = /\+/g,  // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
        query  = window.location.search.substring(1);

    urlParams = {};
    while (match = search.exec(query))
       urlParams[decode(match[1])] = decode(match[2]);
})();

$( document ).ready( function() {

   $( '#myModal' ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
   $( "#signinForm" ).submit( login.authenticate );
   
   var msg = urlParams['confirmRegistration'];
   if ( msg == "true" ) {
      utility.showMessage( "Your account is now enabled. Log in to continue", $("#signinMessage"));
   } else if (msg == "false"){
      utility.showMessage( "Sorry, your registration could not be validated. Please register again.", $("#signinMessage"));
   }


});

