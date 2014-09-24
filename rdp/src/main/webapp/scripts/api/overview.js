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
overview.geneTableHTMLBlock = function(taxon, id) {
   var htmlBlock =  '<div id="' + id + 'Block" class="form-group"> \
                        <div class = "col-sm-offset-3 col-sm-6 text-center"> \
                           <h4>' + taxon + '</h4> \
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
                                    <th>Primary?</th> \
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
populateTable = function(id, data, max, editable) {
   var taxonId = data[0].taxon.replace(/ /g,'').replace(/\./g,'');
   
   $( "#" + id + " tbody tr" ).remove();
   max = Math.min( max, data.length );
   var url;
   var urlBase = "http://www.ncbi.nlm.nih.gov/gene/"
   for ( var i = 0; i < max; i++ ) {   
      url = urlBase + data[i].ncbiGeneId;
      var tier = "";
      if (data[i].tier) {
         tier = data[i].tier === "TIER1" ? '<span class="glyphicon glyphicon-ok"></span>' : "";
      }
      $( '#' +id + '> tbody:last' ).append( '<tr><td><a href="' + url + '" target="_blank">'+ data[i].officialSymbol + '</a></td><td>'+ data[i].officialName + '</td><td>'+ tier + '</td></tr>' );
   }
   
   var seeAllButtonHTML = ( max < data.length ) ? '<button type="button" id="overview' + taxonId + 'Button" \
                                                      class="btn btn-default btn-xs" data-toggle="tooltip" \
                                                      data-placement="bottom" title="See all genes"> \
                                                      <span>See All</span> \
                                                   </button>' : '';
   
   var editButton = ( editable ) ? '<button type="button" id="overviewEdit' + taxonId + 'Button" \
                                    class="btn btn-default btn-xs" data-toggle="tooltip" \
                                    data-placement="bottom" title="Edit genes"> \
                                    <span>Edit</span> \
                                 </button>' : '' ;
   
   if ( max < data.length ) {
      $( '#' +id + '> tbody:last' ).append( '<tr><td class="text-center">...</td> \
                                                 <td class="text-center">...</td></tr>' );
   }
   if ( editable ) {
      $( '#' +id + 'Block' ).after( '<div class="form-group"><div class="col-sm-offset-8 col-sm-4">' + 
                                     editButton + seeAllButtonHTML + '</div></div>' );
   }
   
};

overview.showProfile = function() {
    //Fill in overview profile
	
	$('#overviewName').text( researcherModel.getFirstName() + " " + researcherModel.getLastName() );
    $('#overviewEmail').text( researcherModel.getEmail() );
    $('#overviewOrganisation').text( researcherModel.getOrganization() );
    if ( researcherModel.getWebsite() !== "" ) {
       $('#overviewURL').html( "<a href='" + researcherModel.getWebsite() + "' target='_blank'>"+ researcherModel.getWebsite() + "</a>" );
    }
    $('#overviewFocus').text( researcherModel.getDescription() );
    
   if ( researcherModel.getFirstName() === ""  && researcherModel.getLastName() === "" ) {
	   showMessage( "<a href='#editProfileModal' class='alert-link' data-toggle='modal'>Missing contact details - Click Here</a>", $("#overviewMessage") );
   }
   else {
	   hideMessage( $("#overviewMessage") );
   }
}

overview.showGenes = function() {
	$('#overviewGeneBreakdown').html('');
	var genes = researcherModel.getGenes();
	var genesByTaxon = {};
	var allTaxons = [];
	
	
	//Fill in overview genes
	// Bucket response data by taxon
	for (var i = 0; i < genes.length; i++) {
	   if(genes[i].taxon in genesByTaxon){
	      genesByTaxon[genes[i].taxon].push(genes[i]);
	   }
	   else {
	      allTaxons.push( genes[i].taxon );
	      genesByTaxon[genes[i].taxon] = [ genes[i] ];
	   }
	}

	allTaxons.sort(); // Consistent ordering
	
	// Generate HTML blocks for each taxon
	for (var i=0; i<allTaxons.length; i++) {
	   taxon = allTaxons[i];
	   var taxonId = taxon.replace(/ /g,'').replace(/\./g,'');
	   var taxonDescription = researcherModel.getTaxonDescriptions()[taxon];
	   $('#overviewGeneBreakdown').append(overview.geneTableHTMLBlock(taxon, 'overviewTable'+taxonId));
	   
	   if (taxonDescription) {
	      taxonDescription = taxonDescription.length > 100 ? taxonDescription.substring(0,100) + "..." : taxonDescription;
	      $('#'+'overviewTable'+taxonId+'BlockDescription').append("<span><em>"+ taxonDescription +"</em></span>");
	   }
	   
	   populateTable('overviewTable'+taxonId, genesByTaxon[taxon] , 5, true)
	   $("#overview" + taxonId + "Button").click( createModal( taxon, genesByTaxon[taxon] ) ); 
	   $("#overviewEdit" + taxonId + "Button").click( openGeneManager(taxon) ); 
	}
	
   if ( genes.length === 0 ) {
	   showMessage( "<a href='#editGenesModal' class='alert-link' data-toggle='modal'>No model organisms have been added to profile  - Click Here.</a>", $( "#overviewModelMessage" ) );
   }
   else {
	   hideMessage( $("#overviewModelMessage") );
   }
	
}

overview.showOverview = function() {
	overview.showProfile();
	overview.showGenes();
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
            populateTable('scrapModalTable', data , data.length, false)
            scrapModal.find('.modal-header > h4').text(taxon + " Genes Studied").end();
            scrapModal.modal('show');                
   }; 
 };
 
 function openGeneManager( taxon ) {
    return function() {
       $( "#taxonCommonNameSelect" ).val(taxon);
       $( "#editGenesModal" ).modal('show');
    };
    
 };


}( window.overview = window.overview || {}, jQuery ));


   //overview.showGenesOverview();
} );