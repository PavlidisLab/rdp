   /**
    * @memberOf editGenes
    */
(function( editGenes, $, undefined ) {

	var hiddenGenes = [];
	var taxonDescriptionChanged = false;
	
	editGenes.beforeCloseGeneModal = function(event) {
	   if ( genesChanged() ) {
   	   var confirmExit = confirm('Are you sure? You haven\'t saved your changes. Click OK to leave or Cancel to go back and save your changes.'); 
   	   if ( !confirmExit ) {
   	      event.stopPropagation();
   	      return false;
   	   }
	   }
	}
	
	editGenes.closeGeneModal = function() {
	   hideMessage( $( "#geneManagerMessage" ) );
	   $( "#searchGenesSelect" ).select2("val", "");
	   hiddenGenes = [];
	};
	
	editGenes.saveGenes = function() {
		var table = $( "#geneManagerTable" ).DataTable();
		var oldGenes = researcherModel.getGenes();
		var showingGenes = table.columns().data()[0];
		var newGenes = showingGenes.concat( hiddenGenes );
		researcherModel.setGenes( newGenes );
		researcherModel.addTaxonDescription($( "#taxonCommonNameSelect" ).val(), $( "#taxonDescription" ).val() )
        var promise = researcherModel.saveResearcherGenes();
        $.when(promise).done(function() {
        	showMessage( promise.responseJSON.message, $( "#geneManagerMessage" ) );
        	
        	promise = researcherModel.loadResearcher();
        	$.when(promise).done(function() {
	        	overview.showGenes();
	        	editGenes.fillForm();
        	});
        });
	}
	
	genesChanged = function() {
	   var table = $( "#geneManagerTable" ).DataTable();
	   var oldGenes = researcherModel.getGenes();
	   var showingGenes = table.columns().data()[0];
	   var newGenes = showingGenes.concat( hiddenGenes );
	   return taxonDescriptionChanged || !researcherModel.compareGenes(newGenes,oldGenes);
	}
		
	editGenes.fillForm = function() {
	   console.log(researcherModel.getTaxonDescriptions());
	   taxonDescriptionChanged = false;
	   var taxonSel = $( "#taxonCommonNameSelect" );
	   $.data( taxonSel[0] , 'current', taxonSel.val());
		$( "#geneManagerTable" ).DataTable().clear();
		$( "#searchGenesSelect" ).select2("val", "");
		$( "#taxonDescription" ).val(researcherModel.getTaxonDescriptions()[taxonSel.val()]);
		hiddenGenes = [];
		var table = $( "#geneManagerTable" ).DataTable();
		var genes = researcherModel.getGenes();
	    for (var i = 0; i < genes.length; i++) {
	       	if ( genes[i].taxon === taxonSel.val() ) {
	       	   // columns: Symbol, Alias, Name, ncbiGeneId (HIDDEN)
	       	   //geneRow = [ genes[i].officialSymbol, researcherModel.aliasesToString( genes[i] ), genes[i].officialName, genes[i] ];
	       	   geneRow = [genes[i], "",""];
	       	   table.row.add( geneRow );
	       	}
	       	else {
	       		hiddenGenes.push( genes[i] );
	       	}
	    }
	    table.draw();
	}
	
	editGenes.changeOrganism = function() {
	   console.log("changed");
	   var newVal = $(this).val();
	   $( this ).val( $.data(this, 'current') );
	   
	   var changed = genesChanged();
	   
	   var confirmExit = changed ? confirm('Are you sure? You haven\'t saved your changes. Click OK to leave or Cancel to go back and save your changes.') : true; 
		if ( confirmExit ) {
   	   hideMessage( $( "#geneManagerMessage" ) );
         $( this ).val( newVal );
         $.data(this, 'current', newVal );
   		editGenes.fillForm();
		}
		else {
		   $( this ).val( $.data(this, 'current') );
		   return false;
		}
	}
	
	editGenes.changeTaxonDescription = function() {
	   taxonDescriptionChanged = true;
	}
	
	editGenes.addGeneToTable = function(gene, table) {
	   if ( gene == null ) {
		      showMessage( "Please select a gene to add", $( "#geneManagerMessage" ) );
		      return;
	   } else {
	      hideMessage( $( "#geneManagerMessage" ) );
	   }

	   //FIXME column(0) now holds objects
	   if ( table.column(0).data().indexOf(gene.officialSymbol) != -1 ) {
	      showMessage( "Gene already added", $( "#geneManagerMessage" ) );
	      return;
	   }
	   
	   // columns: Symbol, Alias, Name, ncbiGeneId (HIDDEN)
	   //researcherModel.addGene(gene);
	   //geneRow = [ gene.officialSymbol, researcherModel.aliasesToString( gene ), gene.officialName, gene.ncbiGeneId ];
	   geneRow = [gene, "",""];
	   table.row.add( geneRow ).draw();
		
	}
	editGenes.addSelectedGene = function() {
		
	   var gene = $( "#searchGenesSelect" ).select2( "data" );
	   var table = $( "#geneManagerTable" ).DataTable();
	   $( "#searchGenesSelect" ).select2("val", "");
	   editGenes.addGeneToTable(gene,table);
	}
	
	editGenes.removeSelectedRows = function() {
		   var table = $( "#geneManagerTable" ).DataTable();
		   var selectedNodes = table.rows( '.selected' );
		   if ( selectedNodes.data().length == 0 ) {
		      showMessage( "Please select a gene to remove", $( "#geneManagerMessage" ) );
		      return;
		   } else {
		      hideMessage( $( "#geneManagerMessage" ) );
		   }
		   var data = selectedNodes.data();
		   var selectedTaxon = $( "#taxonCommonNameSelect" ).val();
		   for (var i = 0; i < data.length; i++) {
		      //researcherModel.removeGeneOnSave(data[i][0])
		   }
		   selectedNodes.remove().draw( false );
		} 
	
	editGenes.bulkImportGenes = function() {
       var geneSymbols = $( "#importGeneSymbolsTextArea" ).val();
	   var taxon = $( "#taxonCommonNameSelect" ).val();
	   var table = $( "#geneManagerTable" ).DataTable();
	   var promise = $.ajax( {
	      url : "findGenesByGeneSymbols.html",
	      dataType : "json",
	      data : {
	         "symbols" : geneSymbols,
	         "taxon" : taxon
	      },
	      success : function(response, xhr) {

	         //$( "#spinImportGenesButton" ).hide();

	         // convert object to text symbol + text
	         // select2 format result looks for the 'text' attr
	         for (var i = 0; i < response.data[0].length; i++) {
	            gene = response.data[0][i];
	            editGenes.addGeneToTable(gene,table);
	         }

	         showMessage( response.message, $( "#geneManagerMessage" ) );

	      },
	      error : function(response, xhr) {

	         //$( "#spinImportGenesButton" ).hide();

	         showMessage( response.message, $( "#geneManagerMessage" ) );
	      }

	   } );
	   
	   return promise;
	}
	
	editGenes.initDataTable = function() {
	   // Initialize datatable
	   $( "#geneManagerTable" ).dataTable( {
	      //"scrollX": true,
	      "searching": false,
	      dom: 'T<"clear">lfrtip',
	      tableTools: {
	          "sRowSelect": "os",
	          "aButtons": [ {"sExtends":    "text", "fnClick":editGenes.removeSelectedRows, "sButtonText": "Remove Selected" },
	                        "select_all", 
	                        "select_none" ]
	      },
	      "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
	         $('td:eq(0)', nRow).html('<a href="' + "http://www.ncbi.nlm.nih.gov/gene/" + aData[0].ncbiGeneId + '" target="_blank">'+ aData[0].officialSymbol + '</a>');
	         $('td:eq(1)', nRow).html(researcherModel.aliasesToString( aData[0] ));
	         $('td:eq(2)', nRow).html(aData[0].officialName);
	         //$('td:eq(1)', nRow).html(aData[1].replace( /,/g, ", " )); // DataTables causes some visual bugs when there are no spaces
	         return nRow;
	      },
	      
	   } );
	}
	
	editGenes.initSelect2 = function(config) {
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
                  aliasStr = gene.aliases.length > 0 ? "(" + researcherModel.aliasesToString( gene ) + ") " : "";
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
	   } );
	}
	
}( window.editGenes = window.editGenes || {}, jQuery ));

$( document ).ready( function() {
	editGenes.initDataTable();
	editGenes.initSelect2({
		      'container' : $( "#searchGenesSelect" ),
		      'taxonEl' : $( "#taxonCommonNameSelect" )
		   });
	$( "#editGenesModal .saveGenesButton" ).click( editGenes.saveGenes );
	$( '#editGenesModal' ).on( 'show.bs.modal', editGenes.fillForm );
	$( '#editGenesModal' ).on( 'hide.bs.modal', editGenes.beforeCloseGeneModal );
	$( '#editGenesModal' ).on( 'hidden.bs.modal', editGenes.closeGeneModal );
	$( '#taxonCommonNameSelect').on('change', editGenes.changeOrganism);
	$( "#addGeneButton" ).click( editGenes.addSelectedGene );
	$( "#clearImportGenesButton" ).click( function() {
		   $( "#importGeneSymbolsTextArea" ).val( '' );
		} );
	$( "#importGenesButton" ).click(editGenes.bulkImportGenes);
	$( "#taxonDescription" ).on('change', editGenes.changeTaxonDescription);
	
});