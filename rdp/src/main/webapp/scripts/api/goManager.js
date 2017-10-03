/**
 * @memberOf goManager
 */
(function( goManager, $, undefined ) {
      
	sizeLimit = 100;
	
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

   };
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
   
   goManager.loadSuggestedGOTerms = function() {
      return utility.executeAjax( "getRelatedTerms.html", {'minimumFrequency':2,'minimumTermSize':5,'maximumTermSize':100,'taxonId':modelOrganisms.currentTaxonId()} );
   };
   
   getGOTermStats = function(geneOntologyId) {
      return utility.executeAjax( "getGOTermStats.html", {'geneOntologyId':geneOntologyId, 'taxonId':modelOrganisms.currentTaxonId()}, false );
   };
   
   getGOTermsStats = function(geneOntologyIds) {
      return utility.executeAjax( "getGOTermsStats.html", {'geneOntologyIds':geneOntologyIds, 'taxonId':modelOrganisms.currentTaxonId()}, false );
   };
   
   goManager.getGenePool = function(geneOntologyId) {
      return utility.executeAjax( "getGenePool.html", {'geneOntologyId':geneOntologyId, 'taxonId':modelOrganisms.currentTaxonId()}, false );
   };
   
   //goManager.alertFilter = "#modelOrganisms .main-header .alert div";
   goManager.alertFilter = "#modelOrganisms #go-tab .alert div";

   saveGoTerms = function() {      
      modelOrganisms.lockAll();
      if (!goManager.isChanged() ) {
         // no changes
         return null;
      }
      var btn = $(this);
      btn.attr("disabled", "disabled");
      btn.children('i').addClass('fa-spin');

      var researcher = researcherModel.currentResearcher;
      
      var taxonId = modelOrganisms.currentTaxonId();
      var newTerms = goManager.table().DataTable().columns().data()[0];
      
      researcher.addTaxonDescription(taxonId, modelOrganisms.focus().text().replace(/\s/g, " ").trim() );
      researcher.updateTermsForTaxon( newTerms, taxonId );
      var promise = researcherModel.saveResearcherTermsForTaxon( taxonId );
      
      // invalidate all child rows so that ajax will be called again on them
      var dTable = goManager.table().DataTable();
      var rows = dTable.rows().nodes();
      for (var i=0;i<rows.length;i++) {
         dTable.row(rows[i]).child.remove();
      }
      
      $.when(promise).done(function() {
         //console.log("Saved Changes");
         utility.showMessage( promise.responseJSON.message, $( goManager.alertFilter  ) );
         //utility.showMessage( promise.responseJSON.message, $( "#primaryContactMessage" ) );
      }).fail(function() {
         utility.showMessage( "FAILED to save changes", $( goManager.alertFilter  ) );
      }).always(function() {
         btn.removeAttr("disabled");
         btn.children('i').removeClass('fa-spin');
      });
      
      
   };
   
   goManager.isChanged = function() {
      var table = goManager.table().DataTable();
      var researcher = researcherModel.currentResearcher;
      var taxonId = modelOrganisms.currentTaxonId();
      var oldTerms = researcher.terms[ taxonId ] || [];

      var showingTerms = table.columns().data()[0]; 
      var focus = researcher.taxonDescriptions[taxonId] ? researcher.taxonDescriptions[taxonId]:"";
      
      return ( modelOrganisms.focus().text().replace(/\s/g, " ").trim() != focus.replace(/\s/g, " ").trim() ) || !researcher.compareTerms(showingTerms, oldTerms);
   };
   
   goManager.loadTable = function() {
      var dTable = goManager.table().DataTable();
      var terms = researcherModel.currentResearcher.terms[ modelOrganisms.currentTaxonId() ] || [];
      goManager.fillTable(terms, dTable);
   };
   
   goManager.fillTable = function(terms, dTable) {
      dTable.clear();
      for ( var i = 0; i < terms.length; i++ ) {
         var termRow = [terms[i]];
         dTable.row.add( termRow );
      }
      dTable.draw();
   };
   
   selectGoTerm = function(terms) {
      var term = goManager.select2().select2( "data" );
      goManager.select2().select2( "val", "" );
      
      goManager.closeAddTermsModal();
      if (term.size <= sizeLimit) {
    	  var inst = goManager.addGoTermToTable(term, true)
      } else {
    	  utility.showMessage( "GO Term is too large, please select another", $( goManager.alertFilter  ) );
      }
      
      if (inst) {
      
         var promise = getGOTermStats( term.geneOntologyId );
         
         $.when(promise).done(function() {
            term.size = promise.responseJSON.geneSize;
            term.frequency = promise.responseJSON.frequency;
            inst.invalidate().draw();
         });
      }
      
      
      
   };
   
   goManager.addGoTermToTable = function( term, draw ) {
      draw = utility.isUndefined( draw ) ? true : draw;

      if ( term == null ) {
         console.log("Please select a GO Term to add");
         utility.showMessage( "Please select a GO Term to add", $( goManager.alertFilter  ) );
         return;
      } else {
         utility.hideMessage( $( goManager.alertFilter  ) );
      }

      var table = goManager.table().DataTable();
      
      if ( table.column(1).data().indexOf(term.geneOntologyId) != -1 ) {
         console.log("GO Term already added");
         utility.showMessage( "GO Term already added", $( goManager.alertFilter  ) );
         return;
      }
      var row = [term];
      var inst = table.row.add( row );
      if (draw) {
         inst.draw();
      }
      
      return inst

   };
   
   goManager.addSelectedTerms = function() {
      var terms = goManager.select2().select2( "data" );
      goManager.select2().select2( "val", "" );
      
      goManager.closeAddTermsModal();
      
      goManager.addGoTermsToTable(terms, true);

   };
   
   goManager.addGoTermsToTable = function(terms, getStats) {

      if ( terms.length ) {
         var results = {'failed':[],'added':[], 'large':[]};
         var refreshTerms = [];
         var refreshIds = [];
         for (var i = 0; i < terms.length; i++) {
            var term = terms[i];
            var res = goManager.addToTable(term);
            if (!res.success) {
               console.log(res);
               console.log(results);
               console.log(results[res.type]);
               results[res.type].push(res.data);
            } else if ( getStats && res.row ) {
               refreshTerms.push({term: term, row: res.row} );
               refreshIds.push(term.geneOntologyId);
            }
         }
         console.log(refreshTerms);
         var promise = getGOTermsStats( refreshIds );
         $.when(promise).done(function() {
            console.log("promise: ", promise);
            for (var i = 0; i < refreshTerms.length; i++) {
               var term = refreshTerms[i].term;
               var stats = promise.responseJSON[term.geneOntologyId];
               if ( !utility.isUndefined(stats) ) {
                  term.size = stats.geneSize;
                  term.frequency = stats.frequency;
                  refreshTerms[i].row.invalidate().draw(false);
               } else {
                  //Something went wrong.
               }
            }

            
         });

         var delim = ", ";
         var msg = [];
         if (results['failed'].length) {
            msg.push("<p><b>Failed to add</b>: " + results['failed'].join(delim) + "</p>");
         }
         
         if (results['added'].length) {
            msg.push("<p><b>Already added</b>: " + results['added'].join(delim) + "</p>");
         }
         
         if (results['large'].length) {
            msg.push("<p><b>Too large</b>: " + results['large'].join(delim) + "</p>");
         }
         
         if (msg.length) {
            msg = msg.join("\n");
            var successCnt = terms.length - results['failed'].length -results['added'].length-results['large'].length;
            msg += "\n<b>Successfully added</b> " + successCnt +" term(s)."
         } else {
            msg = "<b>Successfully added</b> " + terms.length +" term(s)."
         }
         
         utility.showMessage( msg, $( goManager.alertFilter  ) );
         goManager.table().DataTable().rows().draw(false);
      }
   };
   
   goManager.addToTable = function(term) {
      
      if ( term == null ) {
         console.log("Object is not a Term", term);
         return {success:false, msg:"Object is not a Term", data:term, type:'failed' };
      }
            
      var table = goManager.table().DataTable();
      
      if ( table.column(1).data().indexOf(term.geneOntologyId) != -1 ) {
         console.log("GO Term already added");
         return {success:false, msg:"GO Term already added", data:term.geneOntologyId, type:'added' };
      }
      
      if (term.size > sizeLimit) {
         console.log("GO Term too large");
         return {success:false, msg:"GO Term too large", data:term.geneOntologyId, type:'large' };
      }
      
      termRow = [term];
      var inst = table.row.add( termRow );
      inst.draw(false);
      
      return {success:true, row:inst};
      
   };
   
   goManager.openAddTermsModal = function() {
      var dTable = goManager.suggestionTable().DataTable();
      dTable.clear();
      dTable.settings()[0].oLanguage.sEmptyTable = 'Searching for GO term suggestions <img src="styles/select2-spinner.gif">';
      dTable.draw();
      var promise = goManager.loadSuggestedGOTerms();
      goManager.addTermsModal().modal('show');
      $.when(promise).done(function() {
         // When done loading Go Terms
         if (researcherModel.currentResearcher.getGenesByTaxonId( modelOrganisms.currentTaxonId() ).length > 1 ) {
            dTable.settings()[0].oLanguage.sEmptyTable = "Could not find any GO term suggestions for genes entered";
         }
         else {
            dTable.settings()[0].oLanguage.sEmptyTable = "A minimum of 2 genes are required for GO Term suggestions";
         }
         var terms = promise.responseJSON.terms;
         //goManager.combineWithSavedTerms(terms);
         //console.log("GO Terms", terms);
         
         goManager.fillTable(terms, dTable);
      });
      
   };
   
   goManager.closeAddTermsModal =  function() {
      goManager.addTermsModal().modal('hide');
   };
   
   formatGenePool = function( genes ) {
      var result = '<tr class="child-header">'+
      '<th>Symbol</td>'+
      '<th colspan="4">Name</td>'+
      '</tr>';

      genes.sort(function(a, b){
         if (a.officialSymbol < b.officialSymbol)
            return -1;
         if (a.officialSymbol > b.officialSymbol)
           return 1;
         return 0;
         });
      for (var i=0;i<genes.length;i++){
         var savedGenes = researcherModel.currentResearcher.genes;
         var classColored = "";
         for (var j=0;j<savedGenes.length;j++){
            if ( genes[i].id == savedGenes[j].id ) {
               classColored = "green-row";
               break;
            }
         }
         result +=
            '<tr class="'+classColored+'">'+
            '<td>'+genes[i].officialSymbol+'</td>'+
            '<td colspan="4">'+genes[i].officialName+'</td>'+
            '</tr>'
      }
return $(result);
  };
   
   goManager.formatGenePool = function( genes ) {
      var result = '<table class="table table-bordered table-condensed stripe text-left display" cellpadding="5" cellspacing="0" border="0" width="100%">' +
                     '<thead class="child-header">' +
                     '<tr>'+
                        '<th>Symbol</th>'+
                        '<th>Name</th>'+
                     '</tr>' +
                     '</thead>';
                     
      genes.sort(function(a, b){
         if (a.officialSymbol < b.officialSymbol)
            return -1;
         if (a.officialSymbol > b.officialSymbol)
           return 1;
         return 0;
         });              
      for (var i=0;i<genes.length;i++){
         var savedGenes = researcherModel.currentResearcher.genes;
         var classColored = "";
         for (var j=0;j<savedGenes.length;j++){
            if ( genes[i].id == savedGenes[j].id ) {
               classColored = "green-row";
               break;
            }
         }
         result +=
            '<tr class="'+classColored+'">'+
               '<td>'+genes[i].officialSymbol+'</td>'+
               '<td colspan="4">'+genes[i].officialName+'</td>'+
            '</tr>'
      }
      return result + '</table>';
  };
   
   goManager.removeSelectedRows = function() {
      var table = goManager.table().DataTable();
      var selectedNodes = table.rows( '.selected' );
      if ( selectedNodes.data().length == 0 ) {
         console.log("Please select a genes to remove");
         //utility.showMessage( "Please select a gene to remove", $( "#geneManagerMessage" ) );
         return;
      } else {
         //utility.hideMessage( $( "#geneManagerMessage" ) );
      }
      var data = selectedNodes.data();
      selectedNodes.remove().draw( false );
   };
   
   goManager.addHighlightedTerms = function() {
      var dTable = goManager.suggestionTable().DataTable();
      var selectedNodes = dTable.rows( '.selected' );
      if ( selectedNodes.data().length == 0 ) {
         console.log("Please select a term to add");
         //utility.showMessage( "Please select a gene to remove", $( "#geneManagerMessage" ) );
         return;
      } else {
         //utility.hideMessage( $( "#geneManagerMessage" ) );
      }
      var data = selectedNodes.data();
      goManager.closeAddTermsModal();
      
      for (var i=0;i<data.length;i++) {
         goManager.addGoTermToTable(data[i][0], false)
      }
      goManager.table().DataTable().rows().draw();
      
   };

   goManager.initDataTable = function(table, buttons) {
      // Initialize datatable
      table.dataTable( {
         "oLanguage": {
            //"sEmptyTable": 'Searching for GO term suggestions <img src="styles/select2-spinner.gif">'
            "sEmptyTable": 'No Gene Ontology terms have been added'
          },
         "order": [ 4, "desc" ],
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
                             "class":"details-control",
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
                             "aButtons": buttons
                          },
                          "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                             // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
                             // whereas aData[0] refers to the data in the first column, hidden or not
                             var url = "http://www.ebi.ac.uk/QuickGO/GTerm?id="+aData[0].geneOntologyId+"#term=ancchart";
                             var link;
                             if ( aData[0].definition ) {
                                link = '<a href="'+url+'" data-content="' + aData[0].definition + '" data-container="body" data-toggle="popover" target="_blank">'+aData[0].geneOntologyId+'</a>';
                             }
                             else {
                                link = '<a href="'+url+'" data-content="Unknown Definition" data-container="body" data-toggle="popover" target="_blank">'+aData[0].geneOntologyId+'</a>';
                             }

                             
                             
                             $('td:eq(0)', nRow).html(link);
                             $('td:eq(0) > a', nRow).popover({
                                trigger: 'hover',
                                'placement': 'top'
                             });
                             
                             $('td:eq(1)', nRow).html(goManager.aspectToString( aData[0].aspect));
                             
                             if ( utility.isUndefined( aData[0].size ) ) {
                                $('td:eq(4)', nRow).html('<img src="styles/select2-spinner.gif">');
                             } else {
                                if ( $(nRow).hasClass('shown') ) {
                                   $('td:eq(4)', nRow).html('<i class="fa fa-caret-down fa-fw red-icon"></i> ' + aData[0].size );
                                } else {
                                   $('td:eq(4)', nRow).html('<i class="fa fa-caret-right fa-fw green-icon"></i> ' + aData[0].size );
                                }
                                
                             }
                             
                             if ( utility.isUndefined( aData[0].frequency ) ) {
                                $('td:eq(3)', nRow).html('<img src="styles/select2-spinner.gif">');
                             }
                             
                             return nRow;
                          },

      } );
   };
   
   formatTerm = function(term) {
	   if ( term.size > sizeLimit ) {
		   //$(container).addClass("greyed-out");
		   return "greyed-out";
	   }
	   //return term.text;
	    
   };
   
   goManager.initSelect2 = function() {
      // init search genes combo    
      goManager.select2().select2( {
         multiple: true,
         closeOnSelect:false,
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
                  query : query, // search term
                  taxonId: modelOrganisms.currentTaxonId()
               }
            },
            results : function(data, page) {
               var GOResults = [];
               for (var i = 0; i < data.data.length; i++) {
                  var term = data.data[i];
                  term.text = "<b>" + term.geneOntologyId + "</b> <i>" + goManager.aspectToString(term.aspect) + "</i> " + term.geneOntologyTerm + " (<i>Term Size:</i> " + term.size + ")";
                  GOResults.push(term);
               }
               return {
                  results : GOResults
               };
            },

         },
         formatResultCssClass: formatTerm,
         formatSelection : function(item) {
            item.text = item.text.split('</b>')[0] + '</b>';
            return item.text
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
   };

   goManager.init = function() {
      //$( "#addTermButton" ).click( selectGoTerm );
      $( "#addTermButton" ).click( goManager.addSelectedTerms );
      $('#term-tab-save').click(saveGoTerms);
      

   }

}( window.goManager = window.goManager || {}, jQuery ));

$( document ).ready( function() {
   goManager.init();
   goManager.initDataTable( goManager.table(), [ {"sExtends":    "text", "fnClick":function(){return utility.openAccordian($("#aboutcollapse2-4"))}, "sButtonText": '<a href="#"><i class="fa fa-question-circle"></i></a>' },
                                                 {"sExtends":    "text", "fnClick":goManager.openAddTermsModal, "sButtonText": '<i class="fa fa-plus-circle green-icon"></i>&nbsp; Add GO Term(s)' },
                                           {"sExtends":    "text", "fnClick":goManager.removeSelectedRows, "sButtonText": '<i class="fa fa-minus-circle red-icon"></i>&nbsp; Remove Selected' },
                                           "select_all", 
                                           "select_none" ] );
   goManager.initDataTable( goManager.suggestionTable(), [ {"sExtends":    "text", "fnClick":goManager.addHighlightedTerms, "sButtonText": '<i class="fa fa-plus-circle green-icon"></i>&nbsp; Add Highlighted Term(s)' },
                                                           "select_none" ] );
   //goManager.initSuggestDataTable();
   goManager.initSelect2();
   
   // Add event listener for opening and closing details
   $('#go-tab table tbody, #suggestedTermsTable tbody' ).on('click', 'td.details-control', function () {
       var tr = $(this).closest('tr');
       var dTable = $(this).closest('table').DataTable();
       var row = dTable.row( tr );
       if ( row.child.isShown() ) {
           // This row is already open - close it
           row.child.hide();
           tr.removeClass('shown');
           $('i', this).removeClass('fa-caret-down').addClass('fa-caret-right').removeClass('red-icon').addClass('green-icon');
       }
       else {
           // Open this row
          if ( utility.isUndefined( row.child() ) ) {
             var term = row.data()[0];
             var promise = goManager.getGenePool( term.geneOntologyId );
             $.when(promise).done(function() {
                row.child( goManager.formatGenePool( promise.responseJSON.genePool ), 'child-table' ).show();

                promise = getGOTermStats( term.geneOntologyId );
                
                $.when(promise).done(function() {
                   term.size = promise.responseJSON.geneSize;
                   term.frequency = promise.responseJSON.frequency;
                   row.invalidate().draw(false);
                });
                
             });
          } else {
             row.child.show();
             var childTableRows = $('table tbody tr', row.child() );
             var researcher = researcherModel.currentResearcher;
             var taxonId = modelOrganisms.currentTaxonId();
             childTableRows.each( function( i, el ) {
                var symbol = $('td:first',el).text();
                if ( researcher.hasGeneBySymbolTaxonId( symbol, taxonId ) ) {
                   $(el).addClass('green-row');
                } else {
                   $(el).removeClass('green-row');
                }
             });
             
          }
          
          tr.addClass('shown');
          $('i', this).removeClass('fa-caret-right').addClass('fa-caret-down').removeClass('green-icon').addClass('red-icon');

       }
   } );

   

});