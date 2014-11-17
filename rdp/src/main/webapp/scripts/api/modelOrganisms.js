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
   
   modelOrganisms.load = function(taxonName) {
      $("#currentOrganismBreadcrumb").text(taxonName);
      $("#modelOrganisms .main-header em").text ( taxonName );
      modelOrganisms.setFocus();
      
      geneManager.loadTable();
      goManager.loadTable();
      
      $('a[href="#modelOrganisms"][data-toggle="tab"]').tab('show')
   }
   
   modelOrganisms.isChanged = function() {
      return geneManager.isChanged() || goManager.isChanged();
   }
   
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
      $(this).removeClass('fa-lock').addClass('fa-check-square-o').removeClass('yellow-icon').addClass('green-icon');
      var div = $(this).closest('div');
      
      var a = $(this).closest('a');
      
      a.prop('title', 'Save');
      
      $('.data-editable', div).each( function(idx) {
         $(this).prop('contenteditable',true);
         $(this).addClass('editable');
      });


   }

   lockFocus = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-check-square-o').addClass('fa-lock').removeClass('green-icon').addClass('yellow-icon');
      var div = $(this).closest('div');
      
      var a = $(this).closest('a');
      
      a.prop('title', 'Edit');
      
      $('*[contenteditable="true"]', div).each( function(idx) {
         $(this).removeAttr('contenteditable');
         $(this).removeClass('editable');
      });

   }
   
   modelOrganisms.init = function() {
      $('#research-focus a').on('click', 'i[class~="fa-lock"]', editFocus );
      $('#research-focus a').on('click', 'i[class~="fa-check-square-o"]', lockFocus );
      $('a[href="#modelOrganisms"][data-toggle="tab"]').on('show.bs.tab', function() {
         utility.setConfirmChanges($(this), modelOrganisms.isChanged, $('#myModelOrganismsList > ul > li > a[href="#modelOrganisms"]:contains("'+modelOrganisms.currentTaxon()+'")'));
      } );
      $('a[href="#modelOrganisms"]').on('hidden.bs.tab', function() {
         utility.hideMessage( $( "#modelOrganisms .main-header .alert div" ) );
      });
   }
   
}( window.modelOrganisms = window.modelOrganisms || {}, jQuery ));

$( document ).ready( function() {
   modelOrganisms.init();
});