/**
 * @memberOf selectGeneOntologyTerms
 */
(function( selectGeneOntologyTerms, $, undefined ) {

   selectGeneOntologyTerms.modal;
   selectGeneOntologyTerms.table;
   selectGeneOntologyTerms.title;
   selectGeneOntologyTerms.select;
   selectGeneOntologyTerms.currentTaxonId;

   clearTable = function() {
      selectGeneOntologyTerms.table.DataTable().clear();
   }
   
   selectGeneOntologyTerms.loadSuggestedGOTerms = function(taxonId) {
      selectGeneOntologyTerms.currentTaxonId = taxonId;
      var promise = $.ajax( {
         cache : false,
         url : "getRelatedTerms.html",
         data : {'minimumFrequency':2,'minimumTermSize':10,'maximumTermSize':100,'taxonId':taxonId},
         dataType : "json",
         success : function(response, xhr) {
            console.log(response.message);
         },
         error : function(response, xhr) {
            console.log("Error:",response);
         }
      } );

      return promise;
   }
   
   getGOTermStats = function(geneOntologyId) {
      var promise = $.ajax( {
         cache : false,
         url : "getGOTermStats.html",
         data : {'geneOntologyId':geneOntologyId,
                 'taxonId':selectGeneOntologyTerms.currentTaxonId
                 },
         dataType : "json",
         success : function(response, xhr) {
            //console.log(response.message);
         },
         error : function(response, xhr) {
            console.log("Error:",response);
         }
      } );

      return promise;
   }

   getSelectedTerms = function() {
      var terms = selectGeneOntologyTerms.table.DataTable().columns().data()[0];
      var results = [];
      for ( var i = 0; i < terms.length; i++ ) {
         if ( terms[i].selected ) {
            results.push(terms[i]);
         }
      }
      return results;
   }


   selectGeneOntologyTerms.closeModal = function() {
      $( "#spinLoadGOTerms" ).hide();
      clearTable();
      utility.hideMessage( $( "#selectGeneOntologyTermsMessage" ) );
   };

   selectGeneOntologyTerms.saveGoTerms = function() {
      var terms = getSelectedTerms();
      researcherModel.currentResearcher.updateTermsForTaxon( terms, selectGeneOntologyTerms.currentTaxonId );
      var promise = researcherModel.saveResearcherTermsForTaxon( selectGeneOntologyTerms.currentTaxonId );
      var btns = $( "#selectGeneOntologyTermsButton" );
      btns.attr("disabled", "disabled");
      $.when(promise).done(function() {
         btns.removeAttr("disabled");
         utility.showMessage( promise.responseJSON.message, $( "#selectGeneOntologyTermsMessage" ) );
      });
   }

   selectGeneOntologyTerms.openModal = function() {
      $( "#spinLoadGOTerms" ).show();
   }

   selectGeneOntologyTerms.combineWithSavedTerms =  function(terms) {
      // Modifies array in place.
      // Add and select those suggested terms that the researcher has already saved
      var savedTerms = researcherModel.currentResearcher.terms[ selectGeneOntologyTerms.currentTaxonId ] || [];
      
      for ( var i = 0; i < terms.length; i++ ) {
         terms[i].selected = false;
      }
      
      for ( var i = 0; i < savedTerms.length; i++ ) {
         var found = false;
         for ( var j = 0; j < terms.length; j++ ) {
            if ( terms[j].geneOntologyId === savedTerms[i].geneOntologyId ) {
               found = true;
               terms[j].selected = true;
               break;
            }
         }
         if ( !found ) {
            savedTerms[i].selected = true;
            terms.push( savedTerms[i] );
         }
      }
   }
   
   selectGeneOntologyTerms.fillTable = function(terms) {
      $( "#spinLoadGOTerms" ).hide();
      clearTable();
      for ( var i = 0; i < terms.length; i++ ) {
         var termRow = [terms[i]];
         selectGeneOntologyTerms.table.DataTable().row.add( termRow );
      }
      selectGeneOntologyTerms.table.DataTable().draw();
   }
   
   selectGeneOntologyTerms.addGoTerm = function(terms) {
      var term = selectGeneOntologyTerms.select.select2( "data" );
      selectGeneOntologyTerms.select.select2( "val", "" );
      
      
      if ( term == null ) {
         utility.showMessage( "Please select a GO Term to add", $( "#selectGeneOntologyTermsMessage" ) );
         return;
      } else {
         utility.hideMessage( $( "#selectGeneOntologyTermsMessage" ) );
      }

      if ( selectGeneOntologyTerms.table.DataTable().column(1).data().indexOf(term.geneOntologyId) != -1 ) {
         utility.showMessage( "GO Term already added", $( "#selectGeneOntologyTermsMessage" ) );
         return;
      }

      term.taxonId = selectGeneOntologyTerms.currentTaxonId;
      term.selected = true;
      
      termRow = [term];
      var inst = selectGeneOntologyTerms.table.DataTable().row.add( termRow );
      inst.draw();
      
      var promise = getGOTermStats( term.geneOntologyId );
      
      $.when(promise).done(function() {
         term.size = promise.responseJSON.geneSize;
         term.frequency = promise.responseJSON.frequency;
         inst.invalidate().draw();
      });
      
      
      
   }

   selectGeneOntologyTerms.initDataTable = function() {
      // Initialize datatable
      dataTable = selectGeneOntologyTerms.table.dataTable( {
         "order": [[ 5, "desc" ],[ 3, "desc" ]],
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
                                return utility.isUndefined( source[0].geneOntologyId ) ? "" : source[0].geneOntologyId;
                             }
                          },
                          {
                             "aTargets": [ 2 ],
                             "defaultContent": "",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].geneOntologyTerm ) ? "" : source[0].geneOntologyTerm;
                             }
                          },
                          {
                             "aTargets": [ 3 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].frequency ) ? "" : source[0].frequency;
                             }
                          },
                          {
                             "aTargets": [ 4 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].size ) ? "" : source[0].size;
                             }
                          },
                          {
                             "aTargets": [ 5 ],
                             "defaultContent": "",
                             "sWidth":"12%",
                             "sClass":"text-center datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].selected ) ? "" : source[0].selected;
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
                                   $(nRow).addClass("datatable-selected");
                                }
                                else {
                                   $(nRow).removeClass("datatable-selected");
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
                                $(nRow).addClass("datatable-selected");
                             } else {
                                aData[0].selected = false;
                                $(nRow).removeClass("datatable-selected");
                             }
                             
                             if ( utility.isUndefined( aData[0].size ) ) {
                                $('td:eq(3)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }
                             
                             if ( utility.isUndefined( aData[0].frequency ) ) {
                                $('td:eq(2)', nRow).html('<img src="styles/select2-spinner.gif">');
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
   $( "#selectGeneOntologyTermsAddTermButton" ).click( selectGeneOntologyTerms.addGoTerm );
   selectGeneOntologyTerms.modal.on( 'hidden.bs.modal', selectGeneOntologyTerms.closeModal );
   selectGeneOntologyTerms.modal.on( 'show.bs.modal', selectGeneOntologyTerms.openModal );
   

});