/*
 * Dynamically create and display HTML for model organism bucketed gene data.
 * 
 */

$( document ).ready( function() {

	   /**
	    * @memberOf overview
	    */
(function( overview, $, undefined ) {

/**
 * generate HTML block to display an empty table for a specific model organism
 * @param {string} taxon - Model Organism 
 * @param {string} id - HTML DOM ID, MUST BE UNIQUE!
 * @return {string} HTML block
 */
overview.geneTableHTMLBlock = function(header, id, explicitTiers) {
   var tableHeader = explicitTiers ? "Tier" : "Primary?";
   var htmlBlock =  '<div id="' + id + 'Block" class="form-group"> \
                        <div class = "col-sm-offset-3 col-sm-6 text-center"> \
                           <h4>' + header + '</h4> \
                        </div> \
                        <div id="' + id + 'BlockDescription" class = "col-sm-offset-3 col-sm-6"> \
                        \
                        </div> \
                        <div class="col-sm-offset-3 col-sm-6"> \
                           <table id="' + id + '" class="table table-condensed"> \
                              <thead> \
                                 <tr> \
                                    <th>Symbol</th> \
                                    <th>Name</th> \
                                    <th>'+tableHeader+'</th> \
                                 </tr> \
                              </thead> \
                              <tbody> \
                              </tbody> \
                           </table> \
                        </div> \
                     </div>';
                        
   return htmlBlock;
};

/**
 * Empty and re-populate table with data, table found by DOM ID
 * @param {string} id - HTML DOM ID
 * @param {array} data - Array of objects containing new data -> [{'symbol':'...','name':'...'}]
 * @param {number} max - maximum number of rows to populate
 */
populateTable = function(id, data, max, editable, explicitTiers) {
   $( "#overviewTable"+id + " tbody tr" ).remove();
   max = max ? Math.min( max, data.length ) : data.length;
   var url;
   var urlBase = "http://www.ncbi.nlm.nih.gov/gene/"
   data.sort(function(a, b){
      if (a.tier < b.tier)
         return -1;
      if (a.tier > b.tier)
        return 1;
      return 0;
      });
   for ( var i = 0; i < max; i++ ) {   
      url = urlBase + data[i].ncbiGeneId;
      var tier = "";
      if (data[i].tier) {
         if (explicitTiers) {
            tier = data[i].tier;
         } else {
            tier = data[i].tier === "TIER1" ? '<span class="glyphicon glyphicon-ok"></span>' : "";
         }
      }
      $( "#overviewTable"+id + '> tbody:last' ).append( '<tr><td><a href="' + url + '" target="_blank">'+ data[i].officialSymbol + '</a></td><td>'+ data[i].officialName + '</td><td>'+ tier + '</td></tr>' );
   }
   
   var seeAllButtonHTML = ( max < data.length ) ? '<button type="button" id="overviewSeeAllButton' + id + '" \
                                                      class="btn btn-default btn-xs" data-toggle="tooltip" \
                                                      data-placement="bottom" title="See all genes"> \
                                                      <span>See All</span> \
                                                   </button>' : '';
   
   var editButton = ( editable ) ? '<button type="button" id="overviewEditButton' + id + '" \
                                    class="btn btn-default btn-xs" data-toggle="tooltip" \
                                    data-placement="bottom" title="Edit genes"> \
                                    <span>Edit</span> \
                                 </button>' : '' ;
   
   if ( max < data.length ) {
      $( "#overviewTable"+id + '> tbody:last' ).append( '<tr><td class="text-center">...</td> \
                                                 <td class="text-center">...</td></tr>' );
   }
   if ( editable ) {
      $( "#overviewTable"+id + 'Block' ).after( '<div class="form-group"><div class="col-sm-offset-8 col-sm-4">' + 
                                     editButton + seeAllButtonHTML + '</div></div>' );
   }
   
};

overview.showButtons = function() {
   $('#overviewEditDescriptionButton').show();
   $('*[id^="overviewEditButton"]').show();
   $('*[id^="overviewSeeAllButton"]').show();
}

overview.hideButtons = function() {
   $('#overviewEditDescriptionButton').hide();
   $('*[id^="overviewEditButton"]').hide();
   $('*[id^="overviewSeeAllButton"]').hide();
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

overview.showGenes = function(researcher, showAll, explicitTiers) {
   researcher = researcher || researcherModel.currentResearcher;
   showAll = showAll || false;
   explicitTiers = explicitTiers || false;
	$('#overviewGeneBreakdown').html('');
	var genes = researcher.genes;
	var genesByTaxon = {};
	var allTaxons = [];
	
	
	//Fill in overview genes
	// Bucket response data by taxon
	for (var i = 0; i < genes.length; i++) {
	   if(genes[i].taxonId in genesByTaxon){
	      genesByTaxon[genes[i].taxonId].push(genes[i]);
	   }
	   else {
	      allTaxons.push( genes[i].taxonId );
	      genesByTaxon[genes[i].taxonId] = [ genes[i] ];
	   }
	}

	allTaxons.sort(); // Consistent ordering
	
	// Generate HTML blocks for each taxon
	for (var i=0; i<allTaxons.length; i++) {
	   var taxonId = allTaxons[i];
	   var taxon = $('#taxonCommonNameSelect option[value=' + taxonId + ']').text();
	   //var taxonId = taxon.replace(/ /g,'').replace(/\./g,'');
	   var taxonDescription = researcher.taxonDescriptions[taxonId];
	   $('#overviewGeneBreakdown').append(overview.geneTableHTMLBlock(taxon, 'overviewTable'+taxonId, explicitTiers));
	   
	   if (taxonDescription) {
	      taxonDescription = taxonDescription.length > 100 && !showAll ? taxonDescription.substring(0,100) + "..." : taxonDescription;
	      $('#'+'overviewTable'+taxonId+'BlockDescription').append("<span><em>"+ taxonDescription +"</em></span>");
	   }
	   var maxGenes = showAll ? null : 5;
	   populateTable(taxonId, genesByTaxon[taxonId] , maxGenes, true, explicitTiers)
	   if ( !showAll ) {
	      // This if statement is not necessary but stops data being saved in a closure that will never be used
   	   $("#overviewSeeAllButton" + taxonId).click( createModal( taxon, genesByTaxon[taxonId] ) ); 
   	   $("#overviewEditButton" + taxonId).click( openGeneManager(taxonId) ); 
	   }
	}
	
   if ( genes.length === 0 ) {
      utility.showMessage( "<a href='#editGenesModal' class='alert-link' data-toggle='modal'>No model organisms have been added to profile  - Click Here.</a>", $( "#overviewModelMessage" ) );
   }
   else {
	   utility.hideMessage( $("#overviewModelMessage") );
   }
	
}

overview.showOverview = function(researcher, showAll, explicitTiers) {
	overview.showProfile(researcher);
	overview.showGenes(researcher, showAll, explicitTiers);
}
var scrapModal = $('#scrapModal').modal({
   backdrop: true,
   show: false,
   keyboard: false
 });

function createModal(taxon, data) {
   return function() {
            var tableHTML =  '<div class=" form-group"> \
                                 <div class="col-sm-12"> \
                                          <table id="overviewTable-scrapModalTable" class="table table-condensed"> \
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
            populateTable('-scrapModalTable', data , data.length, false)
            scrapModal.find('.modal-header > h4').text(taxon + " Genes Studied").end();
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


}( window.overview = window.overview || {}, jQuery ));


   //overview.showGenesOverview();
} );