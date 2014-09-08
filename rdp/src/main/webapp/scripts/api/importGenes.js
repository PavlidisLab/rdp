// Import gene symbols

function importGeneSymbols(geneSymbols, taxon, tableEl) {
   console.log( "import gene symbols" );

   //$( "#spinImportGenesButton" ).show();

   $.ajax( {
      url : "findGenesByGeneSymbols.html",
      dataType : "json",
      data : {
         "symbols" : geneSymbols,
         "taxon" : taxon
      },
      success : function(response, xhr) {

         //$( "#spinImportGenesButton" ).hide();

         // convert object to text symbol + text
         // select2 format result looks for the 'text' attr
         for (var i = 0; i < response.data[0].length; i++) {
            gene = response.data[0][i]
            addGene( gene, tableEl.DataTable() )
            saveGeneToTable( gene );
         }

         showMessage( response.message, $( "#geneManagerMessage" ) );

      },
      error : function(response, xhr) {

         //$( "#spinImportGenesButton" ).hide();

         showMessage( response.message, $( "#geneManagerMessage" ) );
      }

   } );

}

$( "#clearImportGenesButton" ).click( function() {
   $( "#importGeneSymbolsTextArea" ).val( '' );
} );

$( "#importGenesButton" ).click(
   function() {
      importGeneSymbols( $( "#importGeneSymbolsTextArea" ).val(), $( "#taxonCommonNameSelect" ).val(),
         $( "#geneManagerTable" ) );
   } );