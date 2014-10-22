/**
 * @memberOf selectGeneOntologyTerms
 */
(function( selectGeneOntologyTerms, $, undefined ) {

   selectGeneOntologyTerms.modal;
   selectGeneOntologyTerms.table;
   selectGeneOntologyTerms.title;
   selectGeneOntologyTerms.select;

   selectGeneOntologyTerms.closeModal = function() {
      $( "#spinLoadGOTerms" ).hide();
   };

   selectGeneOntologyTerms.saveGoTerms = function() {
      selectGeneOntologyTerms.modal.modal('hide');
      //TODO ajax to save tier3 based on terms
   }

   selectGeneOntologyTerms.openModal = function() {
      selectGeneOntologyTerms.table.children('tbody').children('tr').remove();
      $( "#spinLoadGOTerms" ).show();
   }

   selectGeneOntologyTerms.fillTable = function(terms, title) {
      $( "#spinLoadGOTerms" ).hide();

      selectGeneOntologyTerms.title.html(title);
      for ( var i = 0; i < terms.length; i++ ) {
         var termRow = [terms[i]];
         selectGeneOntologyTerms.table.DataTable().row.add( termRow );
      }
      selectGeneOntologyTerms.table.DataTable().draw();
   }

   selectGeneOntologyTerms.initDataTable = function() {
      // Initialize datatable
      dataTable = selectGeneOntologyTerms.table.dataTable( {
         "aoColumnDefs": [ 
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
                                return source[0].geneOntologyId || "";
                             }
                          },
                          {
                             "aTargets": [ 2 ],
                             "defaultContent": "",
                             "mData": function ( source, type, val ) {
                                return source[0].geneOntologyTerm || "";
                             }
                          },
                          {
                             "aTargets": [ 3 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return source[0].frequency || "";
                             }
                          },
                          {
                             "aTargets": [ 4 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return source[0].size || "";
                             }
                          },
                          {
                             "aTargets": [ 5 ],
                             "defaultContent": "",
                             "sWidth":"12%",
                             "sClass":"text-center datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return source[0].selected || "";
                             }
                          }],
                          "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                             // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
                             // whereas aData[0] refers to the data in the first column, hidden or not
                             var url = "http://www.ebi.ac.uk/QuickGO/GTerm?id="+aData[0].geneOntologyId+"#term=ancchart";
                             var link;
                             if ( aData[0].definition ) {
                                link = '<a href="'+url+'" data-content="' + aData[0].definition + '" data-toggle="popover" target="_blank">'+aData[0].geneOntologyId+'</a>';
                             }
                             else {
                                link = '<a href="'+url+'" data-content="Unknown Definition" data-toggle="popover" target="_blank">'+aData[0].geneOntologyId+'</a>';
                             }

                             $('td:eq(0)', nRow).html(link);
                             $('td:eq(0) > a', nRow).popover({
                                trigger: 'hover',
                                'placement': 'left'
                             });
                             
                             var inputHTML = '<input type="checkbox"></input>'
                             
                             $('td:eq(4)', nRow).html(inputHTML);
                             $('td:eq(4)', nRow).unbind('change').on('change', function() {
                                aData[0].selected = $(this)[0].firstChild.checked;
                                if (aData[0].selected) {
                                   $(nRow).css("background-color", "#B5EAAA");
                                }
                                else {
                                   $(nRow).css("background-color", "#FFFFFF");
                                }
                             });
                             $('td:gt(0)', nRow).unbind('click').on('click', function(e) {
                                if( !$( e.target ).is( "input" ) )
                                {
                                   $(nRow).find('input').click();
                                }
                             });
                             
                             if ( aData[0].selected ) {
                                $('td:eq(4)', nRow).find('input').prop("checked",true);
                             } else {
                                aData[0].selected = false;
                             }

                             return nRow;
                          },

      } );
   }
   
   selectGeneOntologyTerms.initSelect2 = function() {
      // init search genes combo    
      selectGeneOntologyTerms.select.select2( {
         id : function(data) {
            return data.geneOntologyId;
         },
         placeholder : "Search for a GO Term",
         minimumInputLength : 3,
         ajax : {
            url : "searchGO.html",
            dataType : "json",
            data : function(query, page) {
               return {
                  query : query // search term
               }
            },
            results : function(data, page) {
               var GOResults = []
               console.log(data);
               for (var i = 0; i < data.data.length; i++) {
                  var term = data.data[i];
                  term.text = "<b>" + term.geneOntologyId + "</b> " + term.geneOntologyTerm;
                  GOResults.push(term);
               }
               return {
                  results : GOResults
               };
            },

         },
         formatAjaxError : function(response) {
            var msg = response.responseText;
            return msg;
         }, 
         // we do not want to escape markup since we are displaying html in results
         escapeMarkup : function(m) {
            return m;
         },
      } );
   }

   selectGeneOntologyTerms.init = function() {
      selectGeneOntologyTerms.modal = $( '#selectGeneOntologyTermsModal');
      selectGeneOntologyTerms.table = $( '#selectGeneOntologyTermsTable');
      selectGeneOntologyTerms.title = $( '#selectGeneOntologyTermsTitle');
      selectGeneOntologyTerms.select = $( '#selectGeneOntologyTermsSelect');
   }
   
}( window.selectGeneOntologyTerms = window.selectGeneOntologyTerms || {}, jQuery ));

$( document ).ready( function() {
   selectGeneOntologyTerms.init();
   selectGeneOntologyTerms.initDataTable();
   selectGeneOntologyTerms.initSelect2();
   $( "#selectGeneOntologyTermsButton" ).click( selectGeneOntologyTerms.saveGoTerms );
   selectGeneOntologyTerms.modal.on( 'hidden.bs.modal', selectGeneOntologyTerms.closeModal );
   selectGeneOntologyTerms.modal.on( 'show.bs.modal', selectGeneOntologyTerms.openModal );

});