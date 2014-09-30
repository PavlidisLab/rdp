/**
 * @memberOf login
 */
(function( login, $, undefined ) {

   login.authenticate = function(e) {
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
               utility.showMessage($("#signinMessage"), "<strong>Warning!</strong> Login email/password incorrect.");
            }
         },
         error : function(response, xhr) {
            console.log( "Error with request. Status is: " + xhr.status );
            console.log( xhr.responseText );
            utility.showMessage($("#signinMessage"), "Error with request. Status is: " + xhr.status);
         }
      } );
      e.preventDefault();
      return false;
   };


}( window.login = window.login || {}, jQuery ));

$( document ).ready( function() {

   $( '#myModal' ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
   $( "#signinForm" ).submit( login.authenticate );


});

