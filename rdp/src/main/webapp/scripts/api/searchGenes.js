/*

Retrieve the list of genes through an AJAX query in a combo box and
displays it as suggestions in a combo box.

 */

// underlines the matching (case-insensitive) text if any
function underline(text, query) {
   var idx = text.toUpperCase().indexOf( query.toUpperCase() );
   if ( idx < 0 )
      return;
   return text.substring( 0, idx ) + "<span class='select2-match'>" + text.substring( idx, idx + query.length )
      + "</span>" + text.substring( idx + query.length, text.length );
}

function dataFormatResult(data, container, query) {
   var idx = data.symbol.toUpperCase().indexOf( query.term.toUpperCase() );
   var markup = data;
   if ( idx >= 0 ) {
      markup = "<b>" + underline( data.symbol, query.term ) + "</b> " + data.name;
   } else {
      markup = "<b>" + data.symbol + "</b> " + underline( data.name, query.term );
   }
   return markup;
}

function dataSortResult(data, container, query) {
   return data.sort( function(a, b) {
      return a.symbol.localeCompare( b.symbol );
   } );
}

$( document ).ready( function() {
   $( "#searchGenesSelect" ).select2( {
      id : function(data) {
         return data.symbol;
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
            return {
               results : data.data
            };
         }
      },
      formatResult : dataFormatResult,
      sortResults : dataSortResult,
   } );

   // FIXME showing undefined after selecting an option
   $( "#searchGenesSelect" ).on( "select2-selecting", function(e) {
      console.log( "selecting val=" + e.val + " choice=" + JSON.stringify( e.choice ) );
   } );

} );
