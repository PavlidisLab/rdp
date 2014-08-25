/*
 * Load and save genes through AJAX queries and display them in a table (DataTables http://www.datatables.net/).
 * 
 */

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

var showMessage = function(response) {
   console.log( $.toJSON( response ) );
   $( "#geneManagerMessage" ).html( response.message );
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

         if ( !response.success ) {
            showMessage( response );
         }

         var tableEl = $( "#geneManagerTable" );

         addRowSelectEvent( tableEl.dataTable() );

         if ( !response.success ) {
            console.log( response.message );
            showMessage( response );
            return;
         }

         showMessage( response );

      },
      error : function(response, xhr) {
         showMessage( response );
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
         var tableEl = $( "#geneManagerTable" );

         if ( !response.success ) {
            console.log( response.message );
            showMessage( response );
            return;
         }

         console.log( "Loaded " + response.data.length + " user genes" )
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

var closeGenesManager = function() {
   $( "#geneManagerFailed" ).hide();

   var tableEl = $( "#geneManagerTable" );

   tableEl.DataTable().clear();

}

var initGeneManager = function() {
   var tableEl = $( "#geneManagerTable" );

   addRowSelectEvent( tableEl.dataTable() );
}

$( document ).ready( function() {

   initGeneManager();

   $( "#geneManagerButton" ).click( showGenes );

   $( "#closeGenesButton" ).click( closeGenesManager );

   $( "#saveGenesButton" ).click( saveGenes );

} );