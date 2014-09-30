/**
 * @memberOf signup
 */
(function( signup, $, undefined ) {
   
   signup.register = function(e) {
      console.log("ajax call")
      $.ajax( {
         cache : false,
         type : 'POST',
         url : "signup.html",
         beforeSend : function(xhr) {
            xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
         },
         data : $( "#signupForm" ).serialize(),
         success : function(response, xhr) {
            utility.showMessage($("#signupMessage"), jQuery.parseJSON( response ).message);
            Recaptcha.reload();
         },
         error : function(xhr) {
            console.log( xhr.responseText );
            utility.showMessage($("#signupMessage"), "Error with request. Status is: " + xhr.status);
            Recaptcha.reload();
         }
      } );
      e.preventDefault();
      return false;
   };
   
}( window.signup = window.signup || {}, jQuery ));

$( document ).ready( function() {
   // Add captcha
   Recaptcha.create( $( "#captchaPublicKey" ).text(), "captchadiv", {
      theme : "clean",
      callback : function() {$("#recaptcha_response_field").prop("required", true);}
   } );
   $( "#signupForm" ).submit( signup.register);

} );
