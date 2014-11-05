   /**
    * @memberOf modelOrganisms
    */
(function( modelOrganisms, $, undefined ) {
   
   modelOrganisms.focus = function() {
      return $('#research-focus .research-focus p');
   }
   
   modelOrganisms.currentTaxon = function() {
      return $( '#currentOrganismBreadcrumb').text();
   };

   modelOrganisms.currentTaxonId = function() {
      return utility.taxonNameToId[ $( '#currentOrganismBreadcrumb').text() ];
   }; 
   
   
   
   modelOrganisms.setFocus = function() {
      var focus = researcherModel.currentResearcher.taxonDescriptions[modelOrganisms.currentTaxonId()] || "";
      modelOrganisms.focus().text( focus );
      modelOrganisms.lockAll();
   }
   
   modelOrganisms.lockAll = function() {
      $('#research-focus a i[class~="fa-check-square-o"]').trigger( "click" )
   }
   
   editFocus = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-edit').addClass('fa-check-square-o').removeClass('yellow-icon').addClass('green-icon');
      var div = $(this).closest('div');
      
      $('.data-editable', div).each( function(idx) {
         $(this).prop('contenteditable',true);
         $(this).addClass('editable');
      });


   }

   lockFocus = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-check-square-o').addClass('fa-edit').removeClass('green-icon').addClass('yellow-icon');
      var div = $(this).closest('div');
      
      $('*[contenteditable="true"]', div).each( function(idx) {
         $(this).removeAttr('contenteditable');
         $(this).removeClass('editable');
      });

   }
   
   modelOrganisms.init = function() {
      $('#research-focus a').on('click', 'i[class~="fa-edit"]', editFocus );
      $('#research-focus a').on('click', 'i[class~="fa-check-square-o"]', lockFocus );
   }
   
}( window.modelOrganisms = window.modelOrganisms || {}, jQuery ));

$( document ).ready( function() {
   modelOrganisms.init();
});