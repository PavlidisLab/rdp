/*
 * Load and save genes through AJAX queries and display them in a table (DataTables http://www.datatables.net/).
 * 
 */

var saveGenes = function() {
   console.log(jQuery.data( $( "#geneManagerTable" )[0] ));
   console.log($.toJSON( jQuery.data( $( "#geneManagerTable" )[0] ) ));
   $.ajax( {
      type: "POST",
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
            console.log( response.message );
            showMessage( response.message, $( "#geneManagerMessage" ) );
            return;
         }
         
         //re-populate overview page
         overview.showGenesOverview();
         showMessage( response.message, $( "#geneManagerMessage" ) );

      },
      error : function(response, xhr) {
         showMessage( response.message, $( "#geneManagerMessage" ) );
      }
   } );
}

var showGenes = function() {
   jQuery.removeData( $( "#geneManagerTable" )[0] );
   $( "#geneManagerTable" ).DataTable().clear();
   $( "#geneManagerTable" ).DataTable().draw();
   $.ajax( {
      url : "loadResearcherGenes.html",

      data : {
         //taxonCommonName : $( "#taxonCommonNameSelect" ).val(),
         taxonCommonName : "All",
      },
      dataType : "json",

      success : function(response, xhr) {
         var tableEl = $( "#geneManagerTable" );

         if ( !response.success ) {
            console.log( response.message );
            showMessage( response.message, $( "#geneManagerMessage" ) );
            return;
         }

         console.log( "Loaded " + response.data.length + " user genes" )
         
         // loop through response data, add gene to table view if it's the 
         // correct taxon otherwise only store it in table data
         for (var i = 0; i < response.data.length; i++) {
            
           	if ( response.data[i].taxon === $( "#taxonCommonNameSelect" ).val() ) {
           		addGene( response.data[i], tableEl.DataTable() );
           	}
           	
           	saveGeneToTable(response.data[i]);
        	
         }

      },
      error : function(response, xhr) {
         showMessage( response.message, $( "#geneManagerMessage" ) );
      }
   } );

};

var closeGenesManager = function() {
   hideMessage( $( "#geneManagerMessage" ) );

   var tableEl = $( "#geneManagerTable" );
   
   // Clear value of select box
   $( "#searchGenesSelect" ).select2("val", "");
   tableEl.DataTable().clear();
}

var refreshGenesManager = function() {
   hideMessage( $( "#geneManagerMessage" ) );
   $( "#searchGenesSelect" ).select2("val", "");
   showGenes();
   
}

var switchModelOrganism = function() {
   hideMessage( $( "#geneManagerMessage" ) );
   $( "#searchGenesSelect" ).select2("val", "");
   $( "#geneManagerTable" ).DataTable().clear();
   $( "#geneManagerTable" ).DataTable().draw();
   
   var tableEl = $( "#geneManagerTable" );
   
   for (var key in jQuery.data( $( "#geneManagerTable" )[0] )) {
      var geneValueObject = jQuery.data( $( "#geneManagerTable" )[0] )[key];
      if ( geneValueObject.taxon === $( "#taxonCommonNameSelect" ).val() ) {
         addGene( geneValueObject, tableEl.DataTable() );
      }
   }
}

var removeRows = function() {
   var table = $( "#geneManagerTable" ).DataTable();
   var selectedNodes = table.rows( '.selected' );
   if ( selectedNodes.data().length == 0 ) {
      showMessage( "Please select a gene to remove", $( "#geneManagerMessage" ) );
      return;
   } else {
      hideMessage( $( "#geneManagerMessage" ) );
   }
   var data = selectedNodes.data();
   var selectedTaxon = $( "#taxonCommonNameSelect" ).val();
   var selectedSymbol;
   for (var i = 0; i < data.length; i++) {
      selectedSymbol = data[i][0]; // data = [symbol, alias, name] ie column heading ordering
      console.log(selectedSymbol);
      jQuery.removeData( $( "#geneManagerTable" )[0], selectedSymbol + ":" + selectedTaxon );
   }
   selectedNodes.remove().draw( false );
} 

$( document ).ready( function() {

   $( 'a[href$="#editGenesModal"]' ).click( showGenes );

   $( '#editGenesModal' ).on( 'hidden.bs.modal', closeGenesManager );

   $( ".saveGenesButton" ).click( saveGenes );

   $('#taxonCommonNameSelect').on('change', switchModelOrganism );   
   
} );