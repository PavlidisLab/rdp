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
   }
   
   editFocus = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-edit').addClass('fa-check-square-o').removeClass('yellow-icon').addClass('green-icon');
      var div = $(this).closest('div');
      var newElem = $('<textarea rows="3" maxlength="1200"></textarea>');
      $('p[class="data-paragraph"]', div).each( function(idx) {
         newElem.text( $(this).text() );
         $(this).replaceWith( newElem.clone() );

      });



   }

   lockFocus = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-check-square-o').addClass('fa-edit').removeClass('green-icon').addClass('yellow-icon');

      var div = $(this).closest('div');
      var newElem = $('<p custom-placeholder=true data-ph="My research on this organism involves..." class="data-paragraph">');
      $('textarea', div).each( function(idx) {
         newElem.text( $(this).val() );
         $(this).replaceWith( newElem.clone() );

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