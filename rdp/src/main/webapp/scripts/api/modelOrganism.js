function showGenesModal() {
   $( "#geneManagerModal" ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
};

$( document ).ready( function() {
   $( "#geneManagerBtn" ).click( showGenesModal );
} );