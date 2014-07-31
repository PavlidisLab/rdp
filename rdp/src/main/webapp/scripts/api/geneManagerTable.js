var jsonToGenesTable = function(response, tableId) {
   $.each( response, function(i, item) {
      $( '<tr>' ).append( $( '<td>' ).text( item.contact.userName ), $( '<td>' ).text( item.contact.email ),
         $( '<td>' ).text( item.contact.firstName ), $( '<td>' ).text( item.contact.lastName ),
         $( '<td>' ).text( item.organization ), $( '<td>' ).text( item.department ) ).appendTo( tableId );
      // console.log($tr.wrap('<p>').html());
   } );
};

var showGenes = function() {

   $.ajax( {
      cache : false,
      type : 'GET',
      url : "loadResearcherGenes.html",
      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         response = $.parseJSON( response );
         var tableId = "#geneManagerTable";
         jsonToGenesTable( response, tableId );

         $( "#geneManagerTable" ).dataTable();
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#geneManagerMessage" ).html(
            "Error with request. Status is: " + xhr.status + ". " + jQuery.parseJSON( response ).message );
         $( "#geneManagerFailed" ).show();
      }
   } );

};

$( document ).ready( function() {

   showGenes();

} );