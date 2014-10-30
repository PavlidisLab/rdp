/**
 * @memberOf geneManager
 */
(function( geneManager, $, undefined ) {

   geneManager.table = function() {
      return $( '#gene-tab table');
   };

   geneManager.currentTaxon = function() {
      return $( '#currentOrganismBreadcrumb').text();
   };

   geneManager.currentTaxonId = function() {
      return utility.taxonNameToId[ $( '#currentOrganismBreadcrumb').text() ];
   };   

   geneManager.fillTable = function() {
      var table = geneManager.table().DataTable();
      table.clear();
      var genes = researcherModel.currentResearcher.genes;
      var currentTaxonId = geneManager.currentTaxonId();
      for (var i = 0; i < genes.length; i++) {
         // This is important; the genes stored in the table are NOT the same instances 
         // as are stored in the currentResearcher. They can be altered without consequence.
         if ( genes[i].tier !== "TIER3" ) {
            var geneClone = genes[i].clone();
            if ( genes[i].taxonId == currentTaxonId ) {
               // columns: Object (HIDDEN), Symbol, Alias, Name, Tier
               geneRow = [geneClone];
               table.row.add( geneRow );
            }
         }
      }
      table.draw();
   }
   
   geneManager.removeSelectedRows = function() {
      var table = geneManager.table().DataTable();
      var selectedNodes = table.rows( '.selected' );
      if ( selectedNodes.data().length == 0 ) {
         //utility.showMessage( "Please select a gene to remove", $( "#geneManagerMessage" ) );
         return;
      } else {
         //utility.hideMessage( $( "#geneManagerMessage" ) );
      }
      var data = selectedNodes.data();
      selectedNodes.remove().draw( false );
   }

   geneManager.initDataTable = function() {
      geneManager.table().dataTable( {
         "oLanguage": {
            "sEmptyTable": 'No genes have been added'
         },
         "order": [[ 4, "asc" ]],
         "aoColumnDefs": [ {
            "defaultContent": "",
            "targets": "_all"
         },
         {
            "aTargets": [ 0 ],
            "defaultContent": "",
            "visible":false,
            "searchable":false
         },
         {
            "aTargets": [ 1 ],
            "defaultContent": "",
            "mData": function ( source, type, val ) {
               return source[0].officialSymbol || "";
            }
         },
         {
            "aTargets": [ 2 ],
            "defaultContent": "",
            "mData": function ( source, type, val ) {
               return source[0].aliasesToString() || "";
            }
         },
         {
            "aTargets": [ 3 ],
            "defaultContent": "",
            "mData": function ( source, type, val ) {
               return source[0].officialName || "";
            }
         },
         {
            "aTargets": [ 4 ],
            "defaultContent": "",
            "sWidth":"12%",
            "sClass":"text-center datatable-checkbox",
            "mData": function ( source, type, val ) {
               return source[0].tier || "";
            }
         }],
         "searching": false,
         dom: 'T<"clear">lfrtip',
         tableTools: {
            "sRowSelect": "os",
            "aButtons": [ {"sExtends":    "text", "fnClick":geneManager.removeSelectedRows, "sButtonText": '<i class="fa fa-minus-circle red-icon"></i>&nbsp; Remove Selected' },
                          "select_all", 
                          "select_none" ]
         },
         "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
            // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
            // whereas aData[0] refers to the data in the first column, hidden or not
            $('td:eq(0)', nRow).html('<a href="' + "http://www.ncbi.nlm.nih.gov/gene/" + aData[0].ncbiGeneId + '" target="_blank">'+ aData[0].officialSymbol + '</a>');
            //$('td:eq(1)', nRow).html(researcherModel.aliasesToString( aData[0] ));
            //$('td:eq(2)', nRow).html(aData[0].officialName);

            var inputHTML = '<input type="checkbox" value="TIER1"></input>'
            $('td:eq(3)', nRow).html(inputHTML);

            if ( aData[0].tier === "TIER1" ) {
               // If this gene has an associated tier and it is TIER1 check the box
               $('td:eq(3)', nRow).find('input').prop("checked",true);
            }
            var table = geneManager.table().DataTable();
            $('td:eq(3)', nRow).on('change', function() {
               aData[0].tier = $(this)[0].firstChild.checked ? "TIER1" : "TIER2";
            });
            //$('td:eq(1)', nRow).html(aData[1].replace( /,/g, ", " )); // DataTables causes some visual bugs when there are no spaces
            return nRow;
         },

      } );

   }


}( window.geneManager = window.geneManager || {}, jQuery ));

$( document ).ready( function() {
   geneManager.initDataTable();


});