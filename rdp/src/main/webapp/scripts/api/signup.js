var register = function() {

   $.ajax( {
      cache : false,
      type : 'POST',
      url : "signup.html",
      beforeSend : function(xhr) {
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      data : $( "#signupForm" ).serialize(),
      success : function(response, xhr) {
         $( "#signupMesssage" ).html( jQuery.parseJSON( response ).message );
         $( "#signupFailed" ).show();
         Recaptcha.reload();
      },
      error : function(xhr) {
         console.log( xhr.responseText );
         $( "#signupMesssage" ).html( "Error with request. Status is: " + xhr.status );
         $( "#signupFailed" ).show();
         Recaptcha.reload();
      }
   } );
};

// Add captcha
Recaptcha.create( $( "#captchaPublicKey" ).text(), "captchadiv", {
   theme : "clean",
   callback : Recaptcha.focus_response_field
} );

// Make sure that the error state element is hidden.
$( "#signupFailed" ).hide();


$( "#signupForm" ).validate( {
   rules : {
      recaptcha_response_field : {
         required : true,
      }
   },
   submitHandler : function(form) {
      register();
      return false;
   }
} );

