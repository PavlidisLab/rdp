/**
 * @memberOf forgotPasswordForm
 */
(function( forgotPasswordForm, $, undefined ) {

   forgotPasswordForm.resetPassword = function(e) {
   e.preventDefault();
   $.ajax( {
      cache : false,
      type : 'POST',
      url : "forgotPassword.html",
      beforeSend : function(xhr) {
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      data : $( "#forgotPasswordForm" ).serialize(),
      success : function(response, xhr) {
         utility.showMessage( jQuery.parseJSON( response ).message, $("#forgotPasswordMessage") );
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         utility.showMessage( "Error with request. Status is: " + xhr.status, $("#forgotPasswordMessage") );
      }
   } );
   
   return false;
};

}( window.forgotPasswordForm = window.forgotPasswordForm || {}, jQuery ));

$( document ).ready( function() {
   $( "#forgotPasswordForm" ).submit( forgotPasswordForm.resetPassword );
} );