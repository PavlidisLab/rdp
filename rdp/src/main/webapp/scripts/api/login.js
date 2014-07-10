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
      error : function(response, xhr) {
         console.log( "Error with request. Status is: " + xhr.status );
         console.log( xhr.responseText );
      }
   } );

   // return false to cancel normal form submit event methods.
   return false;
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

// Initialize form validations
$( "#signinForm" ).validate( {
   submitHandler : function(form) {
      var username = $( "#signinId" ).val();
      var password = $( "#signinPassword" ).val();
      authenticate( username, password );
      return false;
   }
} );


