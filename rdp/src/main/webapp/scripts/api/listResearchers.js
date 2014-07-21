$( document ).ready( function() {

   $( "#navbarUsername" ).on( "loginSuccess", function(event, response) {

      // Only show Researchers tab if current user is "admin"
      if ( jQuery.parseJSON( response ).isAdmin == "true" ) {
         $( "#listResearchersTable" ).dataTable();
         $('#registerTab a[href="#registeredResearchers"]').show();
      }
      
   } );

} );