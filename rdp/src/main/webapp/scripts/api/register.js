//Call the button widget method on the login button to format it.
var logout = function() {
   
   $.ajax( {
      cache : false,
      type : 'POST',
      url : 'j_spring_security_logout',

      success : function(response, xhr) {
         console.log( "logged out successfully" );
         document.location = "login.jsp";
      },
      error : function(xhr) {
         console.log( "log out failed" );
         console.log( xhr.responseText );
      },
      complete : function(result) {
      }

   } );

   // return false to cancel normal form submit event methods.
   return false;
};

$( "#btnLogout" ).click( logout );

