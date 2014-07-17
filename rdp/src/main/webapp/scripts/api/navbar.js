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

var searchSubmit = function() {
   alert( "search submit clicked" );
};

$( "#logout" ).click( logout );
$( "#searchSubmit" ).click( searchSubmit );

// Initialize document
$( "#navbarUsername" ).ready( function() {
   $.ajax( {
      cache : false,
      type : 'GET',
      url : "loadUser.html",
      success : function(response, xhr) {
         $( "#navbarUsername" ).text( jQuery.parseJSON( response ).data.username );
         $( "#navbarUsername" ).append( ' <span class="caret"></span>' );
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#navbarUsername" ).text( "Anonymous" );
         $( "#navbarUsername" ).append( ' <span class="caret"></span>' );
      }
   } );
} );