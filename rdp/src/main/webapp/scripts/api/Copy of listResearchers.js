var jsonToTable = function(response, tableId) {
   $.each( response, function(i, item) {
      $( '<tr>' )
         .append( 
            $( '<td>' ).text( item.name ), 
            $( '<td>' ).text( item.position ), 
            $( '<td>' ).text( item.office ),
            $( '<td>' ).text( item.age ), 
            $( '<td>' ).text( item.startDate ), 
            $( '<td>' ).text( item.salary )
          ).appendTo( tableId );
      // $('#records_table').append($tr);
      // console.log($tr.wrap('<p>').html());
   } );
};

var showResearchers = function() {

   // get this from a controller
   var response = '[{"name":"AAAATest User", "position":"CEO", "office":"Vancouver", "age":"0", "startDate":"0/0/0", "salary":"$0" }]';
   response = $.parseJSON( response );
   var tableId = "#listResearchersTable";
   jsonToTable( response, tableId );

   $( "#listResearchersTable" ).dataTable();
   $( '#registerTab a[href="#registeredResearchers"]' ).show();

   // username, email, first name, last name, organization, department
};

$( document ).ready( function() {

   $( "#navbarUsername" ).on( "loginSuccess", function(event, response) {
      // Only show Researchers tab if current user is "admin"
      if ( jQuery.parseJSON( response ).isAdmin == "true" ) {
         showResearchers();
      }
   } );

} );