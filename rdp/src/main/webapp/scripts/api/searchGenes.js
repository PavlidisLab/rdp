/*
 * Retrieve the list of genes through an AJAX query and
 * display it as suggestions in a combo box (Select2 http://ivaynberg.github.io/select2/).
 */

// sort by symbol attr
//function dataSortResult(data, container, query) {
//   return data.sort( function(a, b) {
//      return a.officialSymbol.localeCompare( b.officialSymbol );
//   } );
//}
function aliasesToString(geneValueObject) {
   arr = [];
   geneValueObject.aliases.forEach( function(ele) {
      arr.push( ele.alias );
   } );
   return arr.join( ',' );
}

// Add and display gene in table
function addGene(geneValueObject, table) {

   if ( geneValueObject == null ) {
      showMessage( "Please select a gene to add", $( "#geneManagerMessage" ) );
      return;
   } else {
      hideMessage( $( "#geneManagerMessage" ) );
   }

   if ( table.column(0).data().indexOf(geneValueObject.officialSymbol) != -1 ) {
      showMessage( "Gene already added", $( "#geneManagerMessage" ) );
      return;
   }
   
   // columns: Symbol, Alias, Name
   geneRow = [ geneValueObject.officialSymbol, aliasesToString( geneValueObject ), geneValueObject.officialName ];

   table.row.add( geneRow ).draw();

}

// Save object to table element using symbol:taxon as key without displaying it
function saveGeneToTable(geneValueObject) {
   jQuery.data( $( "#geneManagerTable" )[0], geneValueObject.officialSymbol + ":" + geneValueObject.taxon,
      geneValueObject );
}

// wrapped search genes in an object for re-usability
var searchGenes = {
   config : {
      'container' : $( "#searchGenesSelect" ),
      'taxonEl' : $( "#taxonCommonNameSelect" )
   },

   'init' : function(config) {
      // init search genes combo
      config.container.select2( {
         id : function(data) {
            return data.officialSymbol;
         },
         placeholder : "Select a gene symbol or name",
         minimumInputLength : 3,
         ajax : {
            url : "searchGenes.html",
            dataType : "json",
            data : function(query, page) {
               return {
                  query : query, // search term
                  taxon : config.taxonEl.val()
               }
            },
            results : function(data, page) {
               // convert object to text symbol + text
               // select2 format result looks for the 'text' attr
               for (var i = 0; i < data.data.length; i++) {
                  gene = data.data[i]
                  aliasStr = gene.aliases.length > 0 ? "(" + aliasesToString( gene ) + ") " : "";
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
      // sortResults : // sort by symbol attr
      // function dataSortResult(data, container, query) {
      // return data.sort( function(a, b) {
      // return a.officialSymbol.localeCompare( b.officialSymbol );
      // } );
      // },
      } );
   }
}

$( document ).ready( function() {
   
   // Initialize datatable
   $( "#geneManagerTable" ).dataTable( {
      //"scrollX": true,
      "searching": false,
      dom: 'T<"clear">lfrtip',
      tableTools: {
          "sRowSelect": "os",
          "aButtons": [ {"sExtends":    "text", "fnClick":removeRows, "sButtonText": "Remove Selected" },
                        "select_all", 
                        "select_none" ]
      }
  } );
   
   // init add genes button
   $( "#addGeneButton" ).click( function() {
      geneValueObject = $( "#searchGenesSelect" ).select2( "data" );
      addGene( geneValueObject, $( "#geneManagerTable" ).DataTable() );
      
      saveGeneToTable( geneValueObject );
   } );

   searchGenes.init( {
      'container' : $( "#searchGenesSelect" ),
      'taxonEl' : $( "#taxonCommonNameSelect" )
   } );

} );
