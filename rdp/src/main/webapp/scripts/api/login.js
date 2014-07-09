var authenticate = function(username, password) {

   // Remember me http://www.baeldung.com/spring-security-remember-me
   // _spring_security_remember_me: rememberMe

   // var rememberMe = form.rememberMe.value;
   $.ajax( {

      cache : false,
      type : 'POST',
      url : "j_spring_security_check",
      async : false,
      data : {
         j_username : username,
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
            document.location = "register.html";
         } else {
            // If the login credentials are not correct,
            // show our error state element.
            $( "#signinFailed" ).show();
         }
      },
      error : function(xhr) {
         console.log( "Error with request. Status is: " + xhr.status );
         console.log( xhr.responseText );
      }
   } );

   // return false to cancel normal form submit event methods.
   return false;
};

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

// ----------------- Initialize objects and plugins -------------

// Initialize document
$( document ).ready( function() {
   $( '#myModal' ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
} );

// Add captcha
Recaptcha.create( $( "#captchaPublicKey" ).text(), "captchadiv", {
   theme : "clean",
   callback : Recaptcha.focus_response_field
} );

// Make sure that the error state element is hidden.
$( "#signinFailed" ).hide();
$( "#signupFailed" ).hide();

// Initialize form validations
$( "#signinForm" ).validate( {
   submitHandler : function(form) {
      var username = $( "#signinId" ).val();
      var password = $( "#signinPassword" ).val();
      authenticate( username, password );
      return false;
   }
} );

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


