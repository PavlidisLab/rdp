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

//Initialize document
$( document ).ready( function() {
   $.ajax( {
      cache : false,
      type : 'GET',
      url : "loadUser.html",
      success : function(response, xhr) {
         var data = jQuery.parseJSON( response ).data;
         var form = $( "#primaryContactForm" );
         form.find("#firstName").val( data.firstName );
         form.find("#lastName").val( data.lastName );
         form.find("#department").val( data.department );
         form.find("#organization").val( data.organization );
         form.find("#email").val( data.email );
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
      }
   } );
} );