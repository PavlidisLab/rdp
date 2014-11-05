/**
 * @memberOf goManager
 */
(function( goManager, $, undefined ) {
   
   goManager.aspectToString = function(aspect) {
      switch(aspect) {
         case "BIOLOGICAL_PROCESS":
            return "BP";
         case "CELLULAR_COMPONENT":
             return "CC";
         case "MOLECULAR_FUNCTION":
            return "MF";
         default:
            return "";
     }

   }
   goManager.table = function() {
      return $( '#go-tab table');
   }; 
   
   goManager.currentTaxon = function() {
      return $( '#currentOrganismBreadcrumb').text();
   };

   goManager.currentTaxonId = function() {
      return utility.taxonNameToId[ $( '#currentOrganismBreadcrumb').text() ];
   };  
   
   goManager.addTermsModal = function() {
      return $( '#addTermsModal');
   };  
   
   goManager.suggestionTable = function() {
      return $( '#suggestedTermsTable');
   };  
   
   goManager.select2 = function() {
      return $( "#searchTermsSelect" );
   }; 

   clearTable = function() {
      goManager.table.DataTable().clear();
   }
   
   goManager.loadSuggestedGOTerms = function(taxonId) {
      goManager.currentTaxonId = taxonId;
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
                 'taxonId':goManager.currentTaxonId
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
      var terms = goManager.table.DataTable().columns().data()[0];
      var results = [];
      for ( var i = 0; i < terms.length; i++ ) {
         if ( terms[i].selected ) {
            results.push(terms[i]);
         }
      }
      return results;
   }

   goManager.saveGoTerms = function() {
      var terms = getSelectedTerms();
      researcherModel.currentResearcher.updateTermsForTaxon( terms, goManager.currentTaxonId );
      var promise = researcherModel.saveResearcherTermsForTaxon( goManager.currentTaxonId );
      var btns = $( "#goManagerButton" );
      btns.attr("disabled", "disabled");
      $.when(promise).done(function() {
         btns.removeAttr("disabled");
         utility.showMessage( promise.responseJSON.message, $( "#goManagerMessage" ) );
         promise = researcherModel.loadResearcher();
         $.when(promise).done(function() {
            // When done reloading researcher
            overview.showOrganisms();
         });
      });
   }

   goManager.combineWithSavedTerms =  function(terms) {
      // Modifies array in place.
      // Add and select those suggested terms that the researcher has already saved
      var savedTerms = researcherModel.currentResearcher.terms[ goManager.currentTaxonId ] || [];
      
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
   
   goManager.load = function(taxonId) {
      clearTable();
      goManager.table.DataTable().settings()[0].oLanguage.sEmptyTable = 'Searching for GO term suggestions <img src="styles/select2-spinner.gif">';
      goManager.table.DataTable().draw();
      var promise = goManager.loadSuggestedGOTerms( taxonId );
      $.when(promise).done(function() {
         // When done loading Go Terms
         goManager.table.DataTable().settings()[0].oLanguage.sEmptyTable = "Could not find any GO term suggestions";
         var terms = promise.responseJSON.terms;
         goManager.combineWithSavedTerms(terms);
         console.log("GO Terms", terms);
         
         goManager.fillTable(terms);
      });
   }
   
   
   goManager.fillTable = function(terms) {
      clearTable();
      for ( var i = 0; i < terms.length; i++ ) {
         var termRow = [terms[i]];
         goManager.table.DataTable().row.add( termRow );
      }
      goManager.table.DataTable().draw();
   }
   
   goManager.addGoTerm = function(terms) {
      var term = goManager.select.select2( "data" );
      goManager.select.select2( "val", "" );
      
      
      if ( term == null ) {
         //utility.showMessage( "Please select a GO Term to add", $( "#goManagerMessage" ) );
         console.log("Please select a GO Term to add")
         return;
      } else {
         //utility.hideMessage( $( "#goManagerMessage" ) );
      }

      if ( goManager.table().DataTable().column(1).data().indexOf(term.geneOntologyId) != -1 ) {
         //utility.showMessage( "GO Term already added", $( "#goManagerMessage" ) );
         console.log("GO Term already added")
         return;
      }

      term.taxonId = goManager.currentTaxonId;
      term.selected = true;
      
      termRow = [term];
      var inst = goManager.table.DataTable().row.add( termRow );
      inst.draw();
      
      var promise = getGOTermStats( term.geneOntologyId );
      
      $.when(promise).done(function() {
         term.size = promise.responseJSON.geneSize;
         term.frequency = promise.responseJSON.frequency;
         inst.invalidate().draw();
      });
      
      
      
   }
   
   goManager.addGoTermToTable = function( term, draw ) {
      draw = utility.isUndefined( draw ) ? true : false;
      if ( !(gene instanceof researcherModel.Gene ) ){
         console.log("Object is not a Gene", gene);
         return;
      }

      if ( gene == null ) {
         console.log("Please select a GO Term to add")
         //utility.showMessage( "Please select a gene to add", $( "#geneManagerMessage" ) );
         return;
      } else {
         //utility.hideMessage( $( "#geneManagerMessage" ) );
      }

      var table = goManager.table().DataTable();
      
      if ( table.column(1).data().indexOf(term.geneOntologyId) != -1 ) {
         console.log("GO Term already added")
         //utility.showMessage( "Gene already added", $( "#geneManagerMessage" ) );
         return;
      }
      var row = [term];
      var inst = table.row.add( row );
      if (draw) {
         inst.draw();
      }

   }
   
   goManager.openAddTermsModal = function() {
      goManager.addTermsModal().modal('show');
   }
   
   goManager.addHighlightedTerms = function() {
      
   }

   goManager.initDataTable = function() {
      // Initialize datatable
      dataTable = goManager.table().dataTable( {
         "oLanguage": {
            //"sEmptyTable": 'Searching for GO term suggestions <img src="styles/select2-spinner.gif">'
            "sEmptyTable": 'No Gene Ontology terms have been added'
          },
         "order": [[ 6, "desc" ],[ 4, "desc" ]],
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
                                return utility.isUndefined( source[0].aspect ) ? "" : source[0].aspect;
                             }
                          },
                          {
                             "aTargets": [ 3 ],
                             "defaultContent": "",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].geneOntologyTerm ) ? "" : source[0].geneOntologyTerm;
                             }
                          },
                          {
                             "aTargets": [ 4 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].frequency ) ? "" : source[0].frequency;
                             }
                          },
                          {
                             "aTargets": [ 5 ],
                             "defaultContent": "",
                             "sClass":"datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].size ) ? "" : source[0].size;
                             }
                          },
                          {
                             "aTargets": [ 6 ],
                             "defaultContent": "",
                             "sWidth":"12%",
                             "sClass":"text-center datatable-checkbox",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].selected ) ? "" : source[0].selected;
                             }
                          }],
                          "searching": false,
                          dom: 'T<"clear">lfrtip',
                          tableTools: {
                             "sRowSelect": "os",
                             "aButtons": [ {"sExtends":    "text", "fnClick":geneManager.openAddGenesModal, "sButtonText": '<i class="fa fa-plus-circle green-icon"></i>&nbsp; Add Gene(s)' },
                                           {"sExtends":    "text", "fnClick":geneManager.removeSelectedRows, "sButtonText": '<i class="fa fa-minus-circle red-icon"></i>&nbsp; Remove Selected' },
                                           "select_all", 
                                           "select_none" ]
                          },
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

                             $('td:eq(1)', nRow).html(goManager.aspectToString( aData[0].aspect));
                             
                             $('td:eq(0)', nRow).html(link);
                             $('td:eq(0) > a', nRow).popover({
                                trigger: 'hover',
                                'placement': 'left'
                             });

                             var inputHTML = '<input type="checkbox"></input>'

                                $('td:eq(5)', nRow).html(inputHTML);
                             $('td:eq(5)', nRow).unbind('change').on('change', function() {
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
                                $('td:eq(5)', nRow).find('input').prop("checked",true);
                                $(nRow).addClass("datatable-selected");
                             } else {
                                aData[0].selected = false;
                                $(nRow).removeClass("datatable-selected");
                             }
                             
                             if ( utility.isUndefined( aData[0].size ) ) {
                                $('td:eq(4)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }
                             
                             if ( utility.isUndefined( aData[0].frequency ) ) {
                                $('td:eq(3)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }

                             return nRow;
                          },

      } );
   }
   
   goManager.initSuggestDataTable = function() {
      // Initialize datatable
      dataTable = goManager.suggestionTable().dataTable( {
         "oLanguage": {
            "sEmptyTable": 'Searching for GO term suggestions <img src="styles/select2-spinner.gif">'
          },
         "order": [[ 6, "desc" ],[ 4, "desc" ]],
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
                                return utility.isUndefined( source[0].aspect ) ? "" : source[0].aspect;
                             }
                          },
                          {
                             "aTargets": [ 3 ],
                             "defaultContent": "",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].geneOntologyTerm ) ? "" : source[0].geneOntologyTerm;
                             }
                          },
                          {
                             "aTargets": [ 4 ],
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].frequency ) ? "" : source[0].frequency;
                             }
                          },
                          {
                             "aTargets": [ 5 ],
                             "defaultContent": "",
                             "mData": function ( source, type, val ) {
                                return utility.isUndefined( source[0].size ) ? "" : source[0].size;
                             }
                          }],
                          "searching": false,
                          dom: 'T<"clear">lfrtip',
                          tableTools: {
                             "sRowSelect": "os",
                             "aButtons": [ {"sExtends":    "text", "fnClick":goManager.addHighlightedTerms, "sButtonText": '<i class="fa fa-plus-circle green-icon"></i>&nbsp; Add Highlighted Term(s)' },
                                           "select_none" ]
                          }
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

                             $('td:eq(1)', nRow).html(selectGeneOntologyTerms.aspectToString( aData[0].aspect));
                             
                             $('td:eq(0)', nRow).html(link);
                             $('td:eq(0) > a', nRow).popover({
                                trigger: 'hover',
                                'placement': 'left'
                             });
                             
                             if ( utility.isUndefined( aData[0].size ) ) {
                                $('td:eq(4)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }
                             
                             if ( utility.isUndefined( aData[0].frequency ) ) {
                                $('td:eq(3)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }

                             return nRow;
                          },

      } );
   }

   goManager.initSelect2 = function() {
      // init search genes combo    
      goManager.select2().select2( {
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
               console.log(data);
               var GOResults = []
               for (var i = 0; i < data.data.length; i++) {
                  var term = data.data[i];
                  term.text = "<b>" + term.geneOntologyId + "</b> <i>" + goManager.aspectToString(term.aspect) + "</i> " +term.geneOntologyTerm;
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

   goManager.init = function() {

   }

}( window.goManager = window.goManager || {}, jQuery ));

$( document ).ready( function() {
   goManager.init();
   goManager.initDataTable();
   goManager.initSuggestDataTable();
   goManager.initSelect2();
   //goManager.initSelect2();
   //$( "#goManagerButton" ).click( goManager.saveGoTerms );
   //$( "#goManagerAddTermButton" ).click( goManager.addGoTerm );
   //goManager.modal.on( 'hidden.bs.modal', goManager.closeModal );
   //goManager.modal.on( 'show.bs.modal', goManager.openModal );
   

});