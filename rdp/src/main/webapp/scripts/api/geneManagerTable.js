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
function addRowSelectEvent(table) {
   table.find( "tbody" ).on( 'click', 'tr', function() {
      if ( $( this ).hasClass( 'selected' ) ) {
         $( this ).removeClass( 'selected' );
      } else {
         table.$( 'tr.selected' ).removeClass( 'selected' );
         $( this ).addClass( 'selected' );
      }
   } );
}

var showError = function(response) {
   console.log( response );
   $( "#geneManagerMessage" ).html( "Error with request: " + response.message );
   $( "#geneManagerFailed" ).show();
}

var saveGenes = function() {
   $.ajax( {
      url : "saveResearcherGenes.html",

      data : {
         genes : $.toJSON( jQuery.data( $( "#geneManagerTable" )[0] ) ),
         // genes : '{ { "BABAM1" : {"ensemblId":"ENSG00000105393","symbol":"BABAM1","name":"BRISC and BRCA1 A
         // complexmember1","label":"BABAM1","geneBioType":"protein_coding","key":"BABAM1:human","taxon":"human","genomicRange":{"baseStart":17378159,"baseEnd":17392058,"label":"19:17378159-17392058","htmlLabel":"19:17378159-17392058","bin":65910,"chromosome":"19","tooltip":"19:17378159-17392058"},"text":"<b>BABAM1</b>BRISC
         // and BRCA1 A complex member 1"} } }',
         taxonCommonName : $( "#taxonCommonNameSelect" ).val(),

      },
      dataType : "json",

      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         // FIXME
         // response = $.parseJSON( response );

         if ( !response.success ) {
            showError( response );
         }

         var tableEl = $( "#geneManagerTable" );

         addRowSelectEvent( tableEl.dataTable() );

         if ( !response.success ) {
            console.log( response.message );
            return;
         }

         // FIXME
         console.log( "found " + response.data.length + " user genes" )
         for (var i = 0; i < response.data.length; i++) {
            addGene( response.data[i], tableEl.DataTable() );
         }

      },
      error : function(response, xhr) {
         showError( response );
      }
   } );
}

var showGenes = function() {

   $.ajax( {
      url : "loadResearcherGenes.html",

      data : {
         taxonCommonName : $( "#taxonCommonNameSelect" ).val(),

      },
      dataType : "json",

      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         // FIXME
         // response = $.parseJSON( response );

         var tableEl = $( "#geneManagerTable" );

         addRowSelectEvent( tableEl.dataTable() );

         if ( !response.success ) {
            console.log( response.message );
            return;
         }

         // FIXME
         console.log( "found " + response.data.length + " user genes" )
         for (var i = 0; i < response.data.length; i++) {
            addGene( response.data[i], tableEl.DataTable() );
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

   $( "#geneManagerButton" ).click( showGenes );

   $( "#saveGenesButton" ).click( saveGenes );

} );