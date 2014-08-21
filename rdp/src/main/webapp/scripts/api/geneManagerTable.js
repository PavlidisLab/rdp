var jsonToGenesTable = function(response, tableId) {
   $.each( response, function(i, item) {
      $( '<tr>' ).append( $( '<td>' ).text( item.contact.userName ), $( '<td>' ).text( item.contact.email ),
         $( '<td>' ).text( item.contact.firstName ), $( '<td>' ).text( item.contact.lastName ),
         $( '<td>' ).text( item.organization ), $( '<td>' ).text( item.department ) ).appendTo( tableId );
      // console.log($tr.wrap('<p>').html());
   } );
};

// Add selected class whenever a row is clicked
// this is useful when we want to delete a row from the table
function addRowSelectEvent( table ) {
   table.find( "tbody" ).on( 'click', 'tr', function() {
      if ( $( this ).hasClass( 'selected' ) ) {
         $( this ).removeClass( 'selected' );
      } else {
         table.$( 'tr.selected' ).removeClass( 'selected' );
         $( this ).addClass( 'selected' );
      }
   } );
}

var showGenes = function() {

   $.ajax( {
      url : "loadResearcherGenes.html",

      data : {
            userName : "ptan",
            taxonCommonName : $( "#taxonCommonNameSelect" ).val(),
    
      },
      dataType : "json",

      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         // FIXME
         //response = $.parseJSON( response );

         var table = $( "#geneManagerTable" ).dataTable();
         
         addRowSelectEvent( table );
         
         if ( !response.success ) {
            console.log( response.message );
            return;
         }

         // FIXME
         console.log("found " + response.data.length + " user genes")
         for ( var i = 0; i < response.data.length; i++ ) {
            addGene( response.data[i], table );
         }

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