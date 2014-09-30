/**
 * @memberOf resetPassword
 */
(function( resetPassword, $, undefined ) {

resetPassword.resetPassword = function(e) {
   $.ajax( {
      cache : false,
      type : 'POST',
      url : "resetPassword.html",
      beforeSend : function(xhr) {
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      data : $( "#resetPasswordForm" ).serialize(),
      success : function(response, xhr) {
         utility.showMessage( $("#resetPasswordMessage") , jQuery.parseJSON( response ).message );
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         utility.showMessage( $("#resetPasswordMessage"), "Error with request. Status is: " + xhr.status );
      }
   } );
   e.preventDefault();
   return false;
};

}( window.resetPassword = window.resetPassword || {}, jQuery ));

$( document ).ready( function() {
   $( "#resetPasswordForm" ).submit( resetPassword.resetPassword );
} );