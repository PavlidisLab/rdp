/*
 * Dynamically create and display HTML for model organism bucketed gene data.
 * 
 */

$( document ).ready( function() {

	   /**
	    * @memberOf overview
	    */
(function( overview, $, undefined ) {

var HTMLBlock = $('<div class="form-group"> \
                        <div class = "col-sm-offset-3 col-sm-6 text-center"> \
                           <h4></h4> \
                        </div> \
                        <div class = "col-sm-offset-3 col-sm-6"> \
                           <span></span>\
                        </div> \
                        <div class="col-sm-offset-3 col-sm-6"> \
                           <table class="table table-condensed"> \
                              <thead> \
                                 <tr> \
                                 </tr> \
                              </thead> \
                              <tbody> \
                              </tbody> \
                           </table> \
                        </div> \
                     </div>');

var seeAllButtonHTML = $('<button type="button" name="seeAll" class="btn btn-default btn-xs" data-toggle="tooltip" \
                           data-placement="bottom" title="See all"> \
                           <span>See All</span> \
                        </button>');

var editButtonHTML = $('<button type="button" name="edit" class="btn btn-default btn-xs" data-toggle="tooltip" \
                        data-placement="bottom" title="Edit"> \
                        <span>Edit</span> \
                      </button>');


overview.showButtons = function() {
   $('#overviewEditDescriptionButton').show();
   $('*[id^="overviewEditButton"]').show();
   $('*[id^="overviewSeeAllButton"]').show();
}

overview.hideButtons = function() {
   // Not really used, but here for history sake
   $('#overviewEditDescriptionButton').hide();
   $('*[id^="overviewEditButton"]').hide();
   $('*[id^="overviewSeeAllButton"]').hide();
   $('*[id^="overviewTermEditButton"]').hide();
   $('*[id^="overviewTermSeeAllButton"]').hide();
}

overview.showProfile = function(researcher) {
    //Fill in overview profile
	researcher = researcher || researcherModel.currentResearcher;
	$('#overviewName').text( researcher.fullName() || "" );
    $('#overviewEmail').text( researcher.email || "" );
    $('#overviewOrganisation').text( researcher.organization || "" );
    if ( researcher.website ) {
       $('#overviewURL').html( "<a href='" + researcher.website + "' target='_blank'>"+ researcher.website + "</a>" );
    } else {
       $('#overviewURL').html("");
    }
    $('#overviewFocus').text( researcher.description || "" );
    
   if ( researcher.fullName() === "" ) {
      utility.showMessage( "<a href='#editProfileModal' class='alert-link' data-toggle='modal'>Missing contact details - Click Here</a>", $("#overviewMessage") );
   }
   else {
	   utility.hideMessage( $("#overviewMessage") );
   }
}

overview.showGenes = function(researcher, showAll, explicitTiers, editable) {
   editable = utility.isUndefined( editable ) ? true : editable;
   researcher = utility.isUndefined( researcher ) ? researcherModel.currentResearcher : researcher;
   showAll = utility.isUndefined( showAll ) ? false : showAll;
   explicitTiers = utility.isUndefined( explicitTiers ) ? false : explicitTiers;
	$('#overviewGeneBreakdown').html('');
	var genes = researcher.genes;
	var genesByTaxon = {};	
	
	//Fill in overview genes
	// Bucket response data by taxon
	for (var i = 0; i < genes.length; i++) {
	   if(genes[i].taxonId in genesByTaxon){
	      genesByTaxon[genes[i].taxonId].push(genes[i]);
	   }
	   else {
	      genesByTaxon[genes[i].taxonId] = [ genes[i] ];
	   }
	}
	
	// Generate HTML blocks for each taxon
	$('#taxonCommonNameSelect option').each( function(idx,val) {
	   var taxonId = $(val).val();
	   var taxon = $(val).text();
	   var taxonDescription = researcher.taxonDescriptions[taxonId];
	   var data = genesByTaxon[taxonId] || [];
	   if (data.length || taxonDescription ) {
   	   var block = HTMLBlock.clone();
   	   $('h4', block).html(taxon);
   	   $('table thead tr', block).append("<th>Symbol</th>");
   	   $('table thead tr', block).append("<th>Name</th>");
   	   if ( explicitTiers ) {
   	      $('table thead tr', block).append("<th>Tier</th>");
   	   } else {
   	      $('table thead tr', block).append('<th width="75px">Primary</th>');
   	   }
   	   
   	   if (taxonDescription) {
   	      taxonDescription = taxonDescription.length > 100 && !showAll ? taxonDescription.substring(0,100) + "..." : taxonDescription;
   	      $('span', block).append("<em>"+ taxonDescription +"</em>");
   	   }
   	   
   	   var maxGenes = showAll ? data.length : Math.min( 5, data.length );
   	   
   	   var url;
   	   var urlBase = "http://www.ncbi.nlm.nih.gov/gene/"
   	   data.sort(function(a, b){
   	      if (a.tier < b.tier)
   	         return -1;
   	      if (a.tier > b.tier)
   	        return 1;
   	      return 0;
   	      });    
   	      
   	   for (var i=0; i<maxGenes; i++ ) {
            url = urlBase + data[i].ncbiGeneId;
            var tier = "";
            if (data[i].tier) {
               if (explicitTiers) {
                  tier = data[i].tier;
               } else {
                  tier = data[i].tier === "TIER1" ? '<span class="glyphicon glyphicon-ok"></span>' : "";
               }
            }
   	      $('tbody:last', block).append('<tr><td><a href="' + url + '" target="_blank">'+ data[i].officialSymbol + '</a></td> \
   	                                    <td>'+ data[i].officialName + '</td> \
   	                                    <td>'+ tier + '</td></tr>')
   	      
   	   }
   	   
         if ( maxGenes < data.length ) {
            $('tbody:last', block).append('<tr><td class="text-center">...</td> \
                                               <td class="text-center">...</td> \
                                               <td class="text-center">...</td></tr>'); 
         }
   	   
         var seeAll = seeAllButtonHTML.clone();
         var edit = editButtonHTML.clone();
         
   	   if ( maxGenes < data.length || editable  ) {
   	      $('tbody:last', block).append('<tr><td colspan="3" class="text-right"></td>');
         }
   	   
         if ( editable ) {
            $('tbody:last tr:last td:last', block).append(edit);
         }
   	   
   	   if ( maxGenes < data.length ) {
   	      $('tbody:last tr:last td:last', block).append(seeAll);
   	   }
         seeAll.click(createGeneModal( taxon, data ));
         edit.click(openGeneManager(taxonId));	   
   	   $('#overviewGeneBreakdown').append(block);

	   }
	});
	
   if ( genes.length === 0 ) {
      utility.showMessage( "<a href='#editGenesModal' class='alert-link' data-toggle='modal'>No model organisms have been added to profile  - Click Here.</a>", $( "#overviewModelMessage" ) );
   }
   else {
	   utility.hideMessage( $("#overviewModelMessage") );
   }
	
}

overview.showTerms = function(researcher, showAll, editable) {
   editable = utility.isUndefined( editable ) ? true : editable;
   researcher = utility.isUndefined( researcher ) ? researcherModel.currentResearcher : researcher;
   showAll = utility.isUndefined( showAll ) ? false : showAll;
   $('#overviewTermBreakdown').html('');
   var termsByTaxon = researcher.terms; 
   
   // Generate HTML blocks for each taxon
   $('#taxonCommonNameSelect option').each( function(idx,val) {
      var taxonId = $(val).val();
      var taxon = $(val).text();
      var data = termsByTaxon[taxonId] || [];

      var taxonDescription = researcher.taxonDescriptions[taxonId];
      if (data.length || taxonDescription ) {
         var block = HTMLBlock.clone();
         $('h4', block).html(taxon);
         $('table thead tr', block).append("<th>GO ID</th>");
         $('table thead tr', block).append("<th>Term</th>");
         $('table thead tr', block).append("<th>Frequency</th>");
         $('table thead tr', block).append("<th>Size</th>");
         
         if (taxonDescription) {
            taxonDescription = taxonDescription.length > 100 && !showAll ? taxonDescription.substring(0,100) + "..." : taxonDescription;
            $('span', block).append("<em>"+ taxonDescription +"</em>");
         }
         
         var maxTerms = showAll ? data.length : Math.min( 5, data.length );
                        
         for (var i=0; i<maxTerms; i++ ) {
            var url = "http://www.ebi.ac.uk/QuickGO/GTerm?id="+data[i].geneOntologyId+"#term=ancchart";
            $('tbody:last', block).append( '<tr><td><a href="' + url + '" target="_blank">'+ data[i].geneOntologyId + '</a></td> \
                                                <td>'+ data[i].geneOntologyTerm + '</td> \
                                                <td>'+ data[i].frequency + '</td> \
                                                <td>'+ data[i].size + '</td></tr>' );
         }
         
         if ( maxTerms < data.length ) {
            $('tbody:last', block).append('<tr><td class="text-center">...</td> \
                                               <td class="text-center">...</td> \
                                               <td class="text-center">...</td> \
                                               <td class="text-center">...</td></tr>'); 
         }
         
         var seeAll = seeAllButtonHTML.clone();
         var edit = editButtonHTML.clone();
         
         if ( maxTerms < data.length || editable  ) {
            $('tbody:last', block).append('<tr><td colspan="4" class="text-right"></td>');
         }
         
         if ( editable ) {
            $('tbody:last tr:last td:last', block).append(edit);
         }
         
         if ( maxTerms < data.length ) {
            $('tbody:last tr:last td:last', block).append(seeAll);
         }
         seeAll.click(createTermModal( taxon, data ));
         edit.click(openTermManager(taxonId));     
         $('#overviewTermBreakdown').append(block);

      }
   });
   
}

overview.showOverview = function(researcher, showAll, explicitTiers, editable) {
	overview.showProfile(researcher);
	overview.showOrganisms(researcher, showAll, explicitTiers, editable);
}

overview.showOrganisms = function(researcher, showAll, explicitTiers, editable) {
   overview.showGenes(researcher, showAll, explicitTiers, editable);
   overview.showTerms(researcher, showAll, editable);
}

var scrapModal = $('#scrapModal').modal({
   backdrop: true,
   show: false,
   keyboard: false
 });

createGeneModal = function(taxon, data) {
   return function() {
            var tableHTML =  '<div class=" form-group"> \
                                 <div class="col-sm-12"> \
                                          <table id="scrapModalTable" class="table table-condensed"> \
                                             <thead> \
                                                <tr> \
                                                   <th>Symbol</th> \
                                                   <th>Name</th> \
                                                   <th>Primary?</th> \
                                                </tr> \
                                             </thead> \
                                             <tbody> \
                                             </tbody> \
                                          </table> \
                                 </div> \
                              </div>'
            $( '#scrapModalFailed' ).nextAll().remove()
            $( '#scrapModalFailed' ).after( tableHTML ); 
            scrapModal.removeClass( "bs-example-modal-sm");
            scrapModal.find(".modal-dialog").removeClass("modal-sm");
            var block = $("#scrapModalTable");
            
            var urlBase = "http://www.ncbi.nlm.nih.gov/gene/"
               
            for (var i=0; i<data.length; i++ ) {
               var url = urlBase + data[i].ncbiGeneId;
               var tier = data[i].tier === "TIER1" ? '<span class="glyphicon glyphicon-ok"></span>' : "";
               $('tbody:last', block).append('<tr><td><a href="' + url + '" target="_blank">'+ data[i].officialSymbol + '</a></td> \
                                             <td>'+ data[i].officialName + '</td> \
                                             <td>'+ tier + '</td></tr>')
               
            }
            
            scrapModal.find('.modal-header > h4').text(taxon + " Genes Studied").end();
            scrapModal.modal('show');                
   }; 
 };
 
createTermModal = function(taxon, data) {
    return function() {
             var tableHTML =  '<div class=" form-group"> \
                                  <div class="col-sm-12"> \
                                           <table id="scrapModalTable" class="table table-condensed"> \
                                              <thead> \
                                                 <tr> \
                                                    <th>GO ID</th> \
                                                    <th>Term</th> \
                                                    <th>Frequency</th> \
                                                    <th>Size</th> \
                                                 </tr> \
                                              </thead> \
                                              <tbody> \
                                              </tbody> \
                                           </table> \
                                  </div> \
                               </div>'
             $( '#scrapModalFailed' ).nextAll().remove()
             $( '#scrapModalFailed' ).after( tableHTML ); 
             scrapModal.removeClass( "bs-example-modal-sm");
             scrapModal.find(".modal-dialog").removeClass("modal-sm");

             var block = $("#scrapModalTable");
             console.log(data);
             for (var i=0; i<data.length; i++ ) {
                var url = "http://www.ebi.ac.uk/QuickGO/GTerm?id="+data[i].geneOntologyId+"#term=ancchart";
                $('tbody:last', block).append( '<tr><td><a href="' + url + '" target="_blank">'+ data[i].geneOntologyId + '</a></td> \
                   <td>'+ data[i].geneOntologyTerm + '</td> \
                   <td>'+ data[i].frequency + '</td> \
                   <td>'+ data[i].size + '</td></tr>' );
             }
             
             scrapModal.find('.modal-header > h4').text(taxon + " GO Terms Studied").end();
             scrapModal.modal('show');                
    }; 
  };
 
 function openGeneManager( taxonId ) {
    return function() {
       $('#taxonCommonNameSelect option[value=' + taxonId + ']').prop('selected',true);
       //$( "#taxonCommonNameSelect" ).val(taxonId);
       $( "#editGenesModal" ).modal('show');
    };
    
 };
 
 function openTermManager( taxonId ) {
    return function() {
       selectGeneOntologyTerms.load(taxonId);
    };
    
 };


}( window.overview = window.overview || {}, jQuery ));


   //overview.showGenesOverview();
} );