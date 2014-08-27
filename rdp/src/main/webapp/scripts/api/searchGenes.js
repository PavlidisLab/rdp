/*
 * Retrieve the list of genes through an AJAX query and
 * display it as suggestions in a combo box (Select2 http://ivaynberg.github.io/select2/).
 */

// sort by symbol attr
function dataSortResult(data, container, query) {
   return data.sort( function(a, b) {
      return a.officialSymbol.localeCompare( b.officialSymbol );
   } );
}

function addGene(geneValueObject, table) {

   // console.log( "addGene = " + geneValueObject.officialName + "; dataTable = " + table );

   // columns: Symbol, Alias, Name
   geneRow = [ geneValueObject.officialSymbol, geneValueObject.aliases, geneValueObject.officialName ];

   table.row.add( geneRow ).draw();

   // save object to table element using symbol as key
   jQuery.data( $( "#geneManagerTable" )[0], geneValueObject.officialSymbol, geneValueObject );

}

$( document ).ready( function() {
   
   // init add genes button
   $( "#addGeneButton" ).click( function() {
      geneValueObject = $( "#searchGenesSelect" ).select2( "data" )
      addGene( geneValueObject, $( "#geneManagerTable" ).DataTable() )
   } );

   // init remove genes button
   $( "#removeGeneButton" ).click( function() {
      var table = $( "#geneManagerTable" ).DataTable();
      var selectedNode = table.row( '.selected' );
      var selectedSymbol = selectedNode.data()[0]; // data = [symbol, alias, name] ie column heading ordering
      selectedNode.remove().draw( false );
      jQuery.removeData( $( "#geneManagerTable" )[0], selectedSymbol );
   } );

   // init search genes combo
   $( "#searchGenesSelect" ).select2( {
      id : function(data) {
         return data.key;
      },
      placeholder : "Enter gene symbol, alias or name",
      minimumInputLength : 3,
      ajax : {
         url : "searchGenes.html",
         dataType : "json",
         data : function(query, page) {
            return {
               query : query, // search term
               taxon : 'human', // FIXME
            }
         },
         results : function(data, page) {
            // convert object to text symbol + text
            // select2 format result looks for the 'text' attr
            for (var i = 0; i < data.data.length; i++) {
               gene = data.data[i]
               aliasStr = gene.aliases.length > 0 ? "(" + gene.aliases + ") " : "";
               gene.text = "<b>" + gene.officialSymbol + "</b> " + aliasStr + gene.officialName
            }
            return {
               results : data.data
            };
         },

      },
      // we do not want to escape markup since we are displaying html in results
      escapeMarkup : function(m) {
         return m;
      },
      sortResults : dataSortResult,
   } );

} );
