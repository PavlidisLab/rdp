var resetPassword = function(username, email) {

   $.ajax( {
      cache : false,
      type : 'POST',
      url : "resetPassword.html",
      beforeSend : function(xhr) {
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      data : $( "#resetPasswordForm" ).serialize(),
      success : function(response, xhr) {
         $( "#resetPasswordMessage" ).html( jQuery.parseJSON( response ).message );
         $( "#resetPasswordFailed" ).show();
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#resetPasswordMessage" ).html( "Error with request. Status is: " + xhr.status );
         $( "#resetPasswordFailed" ).show();
      }
   } );
};

// Initialize form validations
$( "#resetPasswordForm" ).validate( {
   submitHandler : function(form) {
      var username = $( "#resetPasswordId" ).val();
      var email = $( "#resetPasswordEmail" ).val();
      resetPassword( username, email );
      return false;
   }
} );