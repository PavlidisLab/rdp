(function( overview, $, undefined ) {
overview.geneTableHTMLBlock = function(taxon, id) {
   var htmlBlock =  '<div class = "col-sm-offset-4 col-sm-4 text-center"> \
                        <h5>' + taxon + '</h5> \
                     </div> \
                     <div " class = "col-sm-offset-3 col-sm-6"> \
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

overview.populateTable = function(id, data) {
   $("#yourtableid tr").remove();
   for (var i = 0; i < data.length; i++) {      
      $('#' +id + '> tbody:last').append('<tr><td>'+ data[i].symbol + '</td><td>'+ data[i].name + '</td></tr>');
   }
};

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
         var genesByTaxon = {};
         for (var i = 0; i < response.data.length; i++) {
            if(response.data[i].taxon in genesByTaxon){
               genesByTaxon[response.data[i].taxon].count += 1;
               genesByTaxon[response.data[i].taxon].data.push({'symbol':response.data[i].officialSymbol,
                                                               'name':response.data[i].officialName
                                                               });
            }
            else {
               genesByTaxon[response.data[i].taxon] = {'count':1,
                                                       'data':[{'symbol':response.data[i].officialSymbol,
                                                               'name':response.data[i].officialName
                                                             }]
                                                      };
            }
         }
         console.log(genesByTaxon);
         
         for (var taxon in genesByTaxon) {
            $('#overviewGeneBreakdown').append(overview.geneTableHTMLBlock(taxon, 'overviewTable'+taxon));
            overview.populateTable('overviewTable'+taxon, genesByTaxon[taxon].data)
         }
         

      },
      error : function(response, xhr) {
         showMessage( response.message, $( "#overviewMessage" ) );
      }
   } );

};

}( window.overview = window.overview || {}, jQuery ));

$( document ).ready( function() {
   overview.showGenesOverview();
} );