var editUser = function( username, email, password, passwordConfirm, oldpassword ) {

   $.ajax( {
      cache : false,
      type : 'POST',
      url : "editUser.html",
      beforeSend : function(xhr) {
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      data : $( "#changePasswordForm" ).serialize(),
      success : function(response, xhr) {
         $( "#changePasswordMesssage" ).html( jQuery.parseJSON( response ).message );
         $( "#changePasswordFailed" ).show();
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#changePasswordMesssage" ).html( "Error with request. Status is: " + xhr.status );
         $( "#changePasswordFailed" ).show();
      }
   } );
};

// Initialize form validations
$( "#changePasswordForm" ).validate( {
   submitHandler : function(form) {
      var username = $("#username").val();
      var email = $( "#email" ).val();
      var password = $( "#password" ).val();
      var passwordConfirm = $( "#passwordConfirm" ).val();
      var oldpassword = $( "#oldpassword" ).val();
      editUser( username, email, password, passwordConfirm, oldpassword );
      return false;
   }
} );

//Initialize document
$( "#changePasswordForm" ).ready( function() {
   $.ajax( {
      cache : false,
      type : 'GET',
      url : "loadUser.html",
      data : $( "#changePasswordForm" ).serialize(),
      success : function(response, xhr) {
         $( "#email" ).val( jQuery.parseJSON( response ).data.email );
         $( "#username").val( jQuery.parseJSON( response ).data.username );
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#changePasswordMesssage" ).html( "Error with request. Status is: " + xhr.status );
         $( "#changePasswordFailed" ).show();
      }
   });
} );