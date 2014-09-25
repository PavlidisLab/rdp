   /**
    * @memberOf editGenes
    */
(function( editGenes, $, undefined ) {

	var hiddenGenes = [];
	var taxonDescriptionChanged = false;
	var tierChanged = false;
	
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
		var showingGenes = getShowingGenes();	
		var newGenes = showingGenes.concat( hiddenGenes );
		researcherModel.currentResearcher.genes = newGenes.slice();
		researcherModel.currentResearcher.addTaxonDescription($( "#taxonCommonNameSelect" ).val(), $( "#taxonDescription" ).val() )
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
	
	getShowingGenes = function() {
	   // Gets showing Genes and updates them with the selected tiers
	   var table = $( "#geneManagerTable" ).DataTable();
      var showingGenes = table.columns().data()[0];
      var geneTiers = table.columns().nodes()[4];
      for (var i = 0; i < geneTiers.length; i++) {
         if (geneTiers[i].firstChild.checked) {
            showingGenes[i].tier = geneTiers[i].firstChild.value;
         }
         else {
            showingGenes[i].tier = "TIER2";
         }
      }
      return showingGenes;
	}
	
	genesChanged = function() {
	   var table = $( "#geneManagerTable" ).DataTable();
	   var oldGenes = researcherModel.currentResearcher.genes;
	   
	   var showingGenes = getShowingGenes();  
	      
	   var newGenes = showingGenes.concat( hiddenGenes );
	   return taxonDescriptionChanged || !researcherModel.currentResearcher.compareGenes(newGenes);
	}
		
	editGenes.fillForm = function() {
	   taxonDescriptionChanged = false;
	   tierChanged = false;
	   var taxonSel = $( "#taxonCommonNameSelect" );
	   $.data( taxonSel[0] , 'current', taxonSel.val());
		$( "#geneManagerTable" ).DataTable().clear();
		$( "#searchGenesSelect" ).select2("val", "");
		$( "#taxonDescription" ).val(researcherModel.currentResearcher.taxonDescriptions[taxonSel.val()]);
		hiddenGenes = [];
		var table = $( "#geneManagerTable" ).DataTable();
		var genes = researcherModel.currentResearcher.genes;
	    for (var i = 0; i < genes.length; i++) {
	         // This is important; the genes stored in the table are NOT the same instances 
	         // as are stored in the currentResearcher. They can be altered without consequence.
	         var geneClone = genes[i].clone();
	       
	       	if ( genes[i].taxon === taxonSel.val() ) {
	       	   // columns: Object (HIDDEN), Symbol, Alias, Name, Tier
	       	   geneRow = [geneClone];
	       	   table.row.add( geneRow );
	       	}
	       	else {
	       		hiddenGenes.push( geneClone );
	       	}
	    }
	    table.draw();
	    $(".datatable-checkbox > :checkbox").on('change',function() {tierChanged = true})
	}
	
	editGenes.changeOrganism = function() {
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
	   
	   if ( !(gene instanceof researcherModel.Gene ) ){
	      console.log("Object is not a Gene", gene);
	      gene = new researcherModel.Gene(gene);
	   }
	   
	   if ( gene == null ) {
		      showMessage( "Please select a gene to add", $( "#geneManagerMessage" ) );
		      return;
	   } else {
	      hideMessage( $( "#geneManagerMessage" ) );
	   }

	   if ( table.column(1).data().indexOf(gene.officialSymbol) != -1 ) {
	      showMessage( "Gene already added", $( "#geneManagerMessage" ) );
	      return;
	   }
	   
	   // columns: Object (HIDDEN), Symbol, Alias, Name, Tier
	   geneRow = [gene];
	   table.row.add( geneRow ).draw();
		
	}
	editGenes.addSelectedGene = function() {
		
	   var gene = new researcherModel.Gene( $( "#searchGenesSelect" ).select2( "data" ) );
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
	            gene = new researcherModel.Gene( response.data[0][i] );
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
	      "order": [[ 1, "asc" ]],
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
              "sClass":"text-center datatable-checkbox"
            }],
	      "searching": false,
	      dom: 'T<"clear">lfrtip',
	      tableTools: {
	          "sRowSelect": "os",
	          "aButtons": [ {"sExtends":    "text", "fnClick":editGenes.removeSelectedRows, "sButtonText": "Remove Selected" },
	                        "select_all", 
	                        "select_none" ]
	      },
	      "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
	         // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
	         // whereas aData[0] refers to the data in the first column, hidden or not
	         $('td:eq(0)', nRow).html('<a href="' + "http://www.ncbi.nlm.nih.gov/gene/" + aData[0].ncbiGeneId + '" target="_blank">'+ aData[0].officialSymbol + '</a>');
	         //$('td:eq(1)', nRow).html(researcherModel.aliasesToString( aData[0] ));
	         //$('td:eq(2)', nRow).html(aData[0].officialName);
	         
	         var inputHTML = '<input type="checkbox" id="rowSelect'+aData[0].ncbiGeneId+'"value="TIER1"></input>'
	         
	         if ( aData[0].tier ) {
	            // If this gene has an associated tier and it is TIER1 check the box
	            var n = inputHTML.indexOf('value="'+aData[0].tier+'"')
	            if (n > -1) {
	               inputHTML = inputHTML.slice(0, n) + 'checked="checked" ' + inputHTML.slice(n);
	            }
	         }
	         $('td:eq(3)', nRow).html(inputHTML);
	         
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
                  aliasStr = gene.aliases.length > 0 ? "(" + (new researcherModel.Gene(gene)).aliasesToString() + ") " : "";
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