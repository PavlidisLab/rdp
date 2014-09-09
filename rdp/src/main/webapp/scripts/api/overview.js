/*
 * Dynamically create and display HTML for model organism bucketed gene data.
 * 
 */

//namespace: overview
(function( overview, $, undefined ) {

/**
 * generate HTML block to display an empty table for a specific model organism
 * @param {string} taxon - Model Organism 
 * @param {string} id - HTML DOM ID, MUST BE UNIQUE!
 * @return {string} HTML block
 */
overview.geneTableHTMLBlock = function(taxon, id) {
   var htmlBlock =  '<div class = "col-sm-offset-3 col-sm-6 text-center"> \
                        <span>' + taxon + '</span> \
                     </div> \
                     <div class="col-sm-offset-3 col-sm-6"> \
                        <table id="' + id + '" class="table table-condensed"> \
                           <thead> \
                              <tr> \
                                 <th>Symbol</th> \
                                 <th>Name</th> \
                              </tr> \
                           </thead> \
                      \
                           <tbody> \
                           </tbody> \
                        </table> \
                     </div>';
                        
   return htmlBlock;
};

/**
 * Empty and re-populate table with data, table found by DOM ID
 * @param {string} id - HTML DOM ID
 * @param {array} data - Array of objects containing new data -> [{'symbol':'...','name':'...'}]
 * @param {number} max - maximum number of rows to populate
 */
overview.populateTable = function(id, data, max) {
   $( "#" + id + " tbody tr" ).remove();
   max = Math.min( max, data.length );
   var url;
   var urlBase = "http://www.ncbi.nlm.nih.gov/gene/"
   for ( var i = 0; i < max; i++ ) {   
      url = urlBase + data[i].ncbiGeneId;
      $( '#' +id + '> tbody:last' ).append( '<tr><td><a href="' + url + '" target="_blank">'+ data[i].officialSymbol + '</a></td><td>'+ data[i].officialName + '</td></tr>' );
   }
   
   var seeAllButtonHTML = ''
   
   if ( max < data.length ) {
      seeAllButtonHTML = '<button type="button" id="overview' + data[0].taxon + 'Button" \
                              class="btn btn-default btn-xs" data-toggle="tooltip" \
                              data-placement="bottom" title="See all genes"> \
                              <span>See All</span> \
                           </button>'
   }
      $( '#' +id + '> tbody:last' ).append( '<tr><td class="text-center">...</td> \
                                                 <td class="text-right"> ' + seeAllButtonHTML +
                                                   '<button type="button" id="overviewEdit' + data[0].taxon + 'Button" \
                                                      class="btn btn-default btn-xs" data-toggle="tooltip" \
                                                      data-placement="bottom" title="Edit genes"> \
                                                      <span>Edit</span> \
                                                   </button> \
                                                </td></tr>' );

   
};

/**
 * Load researcher genes through AJAX query, bucket results and generate page HTML
 */
overview.showGenesOverview = function() {
   $('#overviewGeneBreakdown').html('');
   $.ajax( {
      url : "loadResearcherGenes.html",

      data : {
         taxonCommonName : "All",
      },
      dataType : "json",

      success : function(response, xhr) {

         if ( !response.success ) {
            console.log( response.message );
            showMessage( response.message, $( "#overviewMessage" ) );
            return;
         }

         console.log( "Showing " + response.data.length + " user genes" )
         
         if (response.data.length == 0) {
            showMessage( "<a href='#editGenesModal' class='alert-link' data-toggle='modal'>No model organisms have been added to profile  - Click Here.</a>", $( "#overviewModalMessage" ) );
            return;
         }
         $( "#overviewModelFailed" ).hide()
         
         var genesByTaxon = {};
         for (var i = 0; i < response.data.length; i++) {
            if(response.data[i].taxon in genesByTaxon){
               genesByTaxon[response.data[i].taxon].count += 1;
               genesByTaxon[response.data[i].taxon].data.push(response.data[i]);
            }
            else {
               genesByTaxon[response.data[i].taxon] = {'count':1,
                                                       'data':[response.data[i]]
                                                      };
            }
         }
         console.log(genesByTaxon);
         
         for (var taxon in genesByTaxon) {
            $('#overviewGeneBreakdown').append(overview.geneTableHTMLBlock(taxon, 'overviewTable'+taxon));
            overview.populateTable('overviewTable'+taxon, genesByTaxon[taxon].data , 5)
            $("#overview" + taxon + "Button").click( createModal( taxon, genesByTaxon[taxon].data ) ); 
            $("#overviewEdit" + taxon + "Button").click( openGeneManager(taxon) ); 
         }

      },
      error : function(response, xhr) {
         showMessage( response.message, $( "#overviewMessage" ) );
      }
   } );

};

var scrapModal = $('#scrapModal').modal({
   backdrop: true,
   show: false,
   keyboard: false
 });

function createModal(taxon, data) {
   return function() {
            overview.populateTable('scrapModalTable', data , data.length)
            scrapModal.find('.modal-header > h4').text(taxon + " Overview").end();
            scrapModal.modal('show');                
   }; 
 };
 
 function openGeneManager( taxon ) {
    return function() {
       $( "#taxonCommonNameSelect" ).val(taxon);
       $( "#editGenesModal" ).modal('show');
       showGenes();
    };
    
 };


}( window.overview = window.overview || {}, jQuery ));

$( document ).ready( function() {
   overview.showGenesOverview();
} );