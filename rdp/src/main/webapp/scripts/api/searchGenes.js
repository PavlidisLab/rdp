/*

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 */

// sort by symbol attr
function dataSortResult(data, container, query) {
   return data.sort( function(a, b) {
      return a.symbol.localeCompare( b.symbol );
   } );
}

function addGene(geneValueObject, table) {

   console.log( "addGene val = " + geneValueObject.name + "; dataTable = " + table );

   // columns: Symbol, Alias, Name
   // FIXME Add Alias
   geneRow = [ geneValueObject.symbol, geneValueObject.symbol, geneValueObject.name ];

   // var table = dataTableEl.DataTable();

   table.row.add( geneRow ).draw();
}

$( document ).ready( function() {

   var table = $( "#geneManagerTable" ).DataTable();

   // init add genes button
   $( "#addGeneBtn" ).click( function() {
      geneValueObject = $( "#searchGenesSelect" ).select2( "data" )
      addGene( geneValueObject, table )
   } );

   // init remove genes button
   $( "#removeGeneBtn" ).click( function() {
      table.row( '.selected' ).remove().draw( false );
   } );

   // init search genes combo
   $( "#searchGenesSelect" ).select2( {
      id : function(data) {
         return data.key;
      },
      placeholder : "Gene symbol",
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
               gene.text = "<b>" + gene.symbol + "</b> " + gene.name
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
