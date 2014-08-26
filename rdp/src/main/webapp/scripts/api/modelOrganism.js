function showGenesModal() {
   $( "#organism-common-name" ).html($("#taxonCommonNameSelect").val());
   $( "#geneManagerModal" ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
};

$( document ).ready( function() {
   $( "#geneManagerButton" ).click( showGenesModal );
} );