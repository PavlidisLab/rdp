function showGenesModal() {
   $( "#geneModalTitle" ).html( $( "#taxonCommonNameSelect" ).val() + " Gene Manager" )
   $( "#geneManagerModal" ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
};

$( document ).ready( function() {
   $( "#geneManagerButton" ).click( showGenesModal );
} );