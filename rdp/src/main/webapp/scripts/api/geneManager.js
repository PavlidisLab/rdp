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
   
   geneManager.addGenesModal = function() {
      return $( '#addGenesModal');
   };  
   
   geneManager.select2 = function() {
      return $( "#searchGenesSelect" );
   };  
   
   
   
   geneManager.isChanged =  function() {
      var table = geneManager.table().DataTable();
      var researcher = researcherModel.currentResearcher;
      var taxonId = geneManager.currentTaxonId();
      var oldGenes = researcher.getGenesByTaxonId( taxonId );

      var showingGenes = table.columns().data()[0]; 
      var focus = researcher.taxonDescriptions[taxonId] ? researcher.taxonDescriptions[taxonId]:"";
      return ( modelOrganisms.focus().text() != focus) || !researcher.compareGenes(showingGenes, oldGenes);
   }
   
   saveGenes = function() {
      modelOrganisms.lockAll();
      if (!geneManager.isChanged() ) {
         return 'no changes';
      }
      var btn = $(this);
      btn.attr("disabled", "disabled");
      btn.children('i').addClass('fa-spin');
      var researcher = researcherModel.currentResearcher;
      
      var taxonId = geneManager.currentTaxonId();
      var newGenes = geneManager.table().DataTable().columns().data()[0];
      
      researcher.setGenesByTaxonId( newGenes, taxonId )
      researcher.addTaxonDescription(taxonId, modelOrganisms.focus().text() )

      var promise = researcherModel.saveResearcherGenesByTaxon(taxonId);

      $.when(promise).done(function() {
         btn.removeAttr("disabled");
         btn.children('i').removeClass('fa-spin');
         console.log("Saved Changes");
         utility.showMessage( promise.responseJSON.message, $( "#modelOrganisms .main-header .alert div" ) );
         //utility.showMessage( promise.responseJSON.message, $( "#primaryContactMessage" ) );
      });
      
   }
   
   geneManager.loadTable = function() {
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
   
   //TODO make this work
   exportGenes  = function() {
      var table = geneManager.table().DataTable();
      var showingGenes = table.columns().data()[0];
      var text = [];
      for (var i=0;i<showingGenes.length;i++) {
         text.push(showingGenes[i].officialSymbol)
      }
      text = text.join("\r\n");
      window.prompt("Copy to clipboard: Ctrl+C (Cmd+C) , Enter", text);
   }
   
   geneManager.openAddGenesModal = function() {
      geneManager.addGenesModal().modal('show');
   }
   
   geneManager.closeAddGenesModal =  function() {
      geneManager.addGenesModal().modal('hide');
   }
   
   geneManager.addGeneToTable = function( gene, draw ) {
      draw = utility.isUndefined( draw ) ? true : draw;
      if ( !(gene instanceof researcherModel.Gene ) ){
         console.log("Object is not a Gene", gene);
         return;
      }

      if ( gene == null ) {
         console.log("Please select a gene to add")
         //utility.showMessage( "Please select a gene to add", $( "#geneManagerMessage" ) );
         return;
      } else {
         //utility.hideMessage( $( "#geneManagerMessage" ) );
      }

      var table = geneManager.table().DataTable();
      
      if ( table.column(1).data().indexOf(gene.officialSymbol) != -1 ) {
         console.log("Gene already added")
         //utility.showMessage( "Gene already added", $( "#geneManagerMessage" ) );
         return;
      }
      geneRow = [gene];
      var inst = table.row.add( geneRow );
      if (draw) {
         inst.draw();
      }

   }
   
   geneManager.addSelectedGene = function() {
      var data = $( "#searchGenesSelect" ).select2( "data" );
      if ( data ) {
         var gene = new researcherModel.Gene( data );
      }
      $( "#searchGenesSelect" ).select2("val", "");
      geneManager.addGeneToTable(gene, true);
      geneManager.closeAddGenesModal();
   }
   
   geneManager.bulkImportGenes = function() {
      var btn = $(this);
      btn.attr("disabled", "disabled");
      btn.children('i').addClass('fa-spin');
      var geneSymbols = $( "#importGeneSymbolsTextArea" ).val();
      var taxonId = geneManager.currentTaxonId();
      var table = geneManager.table().DataTable();
      var promise = $.ajax( {
         url : "findGenesByGeneSymbols.html",
         dataType : "json",
         data : {
            "symbols" : geneSymbols,
            "taxonId" : taxonId
         },
         success : function(response, xhr) {
            btn.removeAttr("disabled");
            btn.children('i').removeClass('fa-spin');

            for (var i = 0; i < response.data[0].length; i++) {
               gene = new researcherModel.Gene( response.data[0][i] );
               geneManager.addGeneToTable( gene, false );
            }
            table.rows().draw();
            console.log(response.message)
            utility.showMessage( promise.responseJSON.message, $( "#modelOrganisms .main-header .alert div" ) );
            //utility.showMessage( response.message, $( "#geneManagerMessage" ) );

         },
         error : function(response, xhr) {
            btn.removeAttr("disabled");
            btn.children('i').removeClass('fa-spin');
            console.log(response.message)
            utility.showMessage( promise.responseJSON.message, $( "#modelOrganisms .main-header .alert div" ) )
            //utility.showMessage( response.message, $( "#geneManagerMessage" ) );
         }

      } );
      geneManager.closeAddGenesModal();
      return promise;
   }

   geneManager.initSelect2 = function() {
      // init search genes combo    
      geneManager.select2().select2( {
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
                  taxonId : geneManager.currentTaxonId()
               }
            },
            results : function(data, page) {
               // convert object to text symbol + text
               // select2 format result looks for the 'text' attr
               var geneResults = []
               for (var i = 0; i < data.data.length; i++) {
                  var gene = new researcherModel.Gene( data.data[i] );
                  var aliasStr = gene.aliasesToString();
                  aliasStr = aliasStr ? "(" + aliasStr + ") " : "";
                  gene.text = "<b>" + gene.officialSymbol + "</b> " + aliasStr + gene.officialName
                  geneResults.push(gene);
               }
               return {
                  results : geneResults
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
            "aButtons": [ {"sExtends":    "text", "fnClick":geneManager.openAddGenesModal, "sButtonText": '<i class="fa fa-plus-circle green-icon"></i>&nbsp; Add Gene(s)' },
                          {"sExtends":    "text", "fnClick":geneManager.removeSelectedRows, "sButtonText": '<i class="fa fa-minus-circle red-icon"></i>&nbsp; Remove Selected' },
                          "select_all", 
                          "select_none"]
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
   
   geneManager.init = function() {
      $( "#addGeneButton" ).click( geneManager.addSelectedGene );
      $( "#clearImportGenesButton" ).click( function() {
         $( "#importGeneSymbolsTextArea" ).val( '' );
      } );
      $( "#importGenesButton" ).click(geneManager.bulkImportGenes);
      $('#gene-tab-save').click(saveGenes);
   }


}( window.geneManager = window.geneManager || {}, jQuery ));

$( document ).ready( function() {
   geneManager.init();
   geneManager.initSelect2();
   geneManager.initDataTable();


});