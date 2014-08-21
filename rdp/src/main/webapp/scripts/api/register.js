//Call the button widget method on the login button to format it.
var saveContact = function() {

   console.log( "primaryContactForm serialized = " + $( "#primaryContactForm" ).serialize() );

   $.ajax( {
      cache : false,
      type : 'POST',
      url : 'saveResearcher.html',
      data : $( "#primaryContactForm" ).serialize(),
      success : function(response, xhr) {
         console.log( "saved researcher successfully" );
         $( "#primaryContactMessage" ).html( jQuery.parseJSON( response ).message );
         $( "#primaryContactFailed" ).show();
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#primaryContactMessage" ).html( jQuery.parseJSON( response ).message );
         $( "#primaryContactFailed" ).show();
      },
      complete : function(result) {
      }

   } );

   // return false to cancel normal form submit event methods.
   return false;
};

$( "#primaryContactForm" ).find( "#submit" ).click( saveContact );

// Initialize document
$( document ).ready( function() {
   $.ajax( {
      //cache : false,
      //type : 'GET',
      url : "loadResearcher.html",
      success : function(response, xhr) {
         
         var data = jQuery.parseJSON( response ).data;
         var form = $( "#primaryContactForm" );

         form.find( "#department" ).val( data.department );
         form.find( "#organization" ).val( data.organization );
         form.find( "#website" ).val( data.website );
         form.find( "#phone" ).val( data.phone );

         // Researcher created so Researcher object was returned
         if ( data.contact != null ) {
            form.find( "#firstName" ).val( data.contact.firstName );
            form.find( "#lastName" ).val( data.contact.lastName );
            form.find( "#email" ).val( data.contact.email );
         } else {
            // Researcher not created yet so User object was returned
            form.find( "#firstName" ).val( data.firstName );
            form.find( "#lastName" ).val( data.lastName );
            form.find( "#email" ).val( data.email );
         }

      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
      }
   } );
} );
