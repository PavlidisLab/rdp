var showMessage = function( message, messageEl ) {
   console.log( message );
   messageEl.html( message );
   messageEl.parent().show();
}

// Call the button widget method on the login button to format it.
var saveContact = function() {

   console.log( "primaryContactForm serialized = " + $( "#primaryContactForm" ).serialize() );

   $.ajax( {
      cache : false,
      type : 'POST',
      url : 'saveResearcher.html',
      data : $( "#primaryContactForm" ).serialize(),
      success : function(response, xhr) {
         // console.log( "saved researcher successfully" );
         // $( "#primaryContactMessage" ).html( jQuery.parseJSON( response ).message );
         // $( "#primaryContactFailed" ).show();
         $('#overviewName').text( $( "#firstName" ).val() + " " + $( "#lastName" ).val() );
         //$('#overviewEmail').text( $( "#email" ).val() );
         $('#overviewOrganisation').text( $( "#organization" ).val() );
         showMessage( jQuery.parseJSON( response ).message, $( "#primaryContactMessage" ) )
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         // $( "#primaryContactMessage" ).html( jQuery.parseJSON( response ).message );
         // $( "#primaryContactFailed" ).show();
         showMessage( jQuery.parseJSON( response ).message, $( "#primaryContactMessage" ) )
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
   
   $(function(){
      $("[data-hide]").on("click", function(){
          $("." + $(this).attr("data-hide")).hide();
          /*
           * The snippet above will hide all elements with the class specified in data-hide,
           * i.e: data-hide="alert" will hide all elements with the alert property.
           *
           * Xeon06 provided an alternative solution:
           * $(this).closest("." + $(this).attr("data-hide")).hide();
           * Use this if are using multiple alerts with the same class since it will only find the closest element
           * 
           * (From jquery doc: For each element in the set, get the first element that matches the selector by
           * testing the element itself and traversing up through its ancestors in the DOM tree.)
          */
      });
   });
   
   $.ajax( {
      // cache : false,
      // type : 'GET',
      url : "loadResearcher.html",
      success : function(response, xhr) {

         var data = jQuery.parseJSON( response ).data;
         var form = $( "#primaryContactForm" );

         form.find( "#department" ).val( data.department );
         form.find( "#organization" ).val( data.organization );
         form.find( "#website" ).val( data.website );
         form.find( "#phone" ).val( data.phone );
         
         var contact;
         
         // Researcher created so Researcher object was returned
         if ( data.contact != null ) {
            contact = data.contact;
            $( "#primaryContactFailed" ).hide();
         } else {
         // Researcher not created yet so User object was returned
            contact = data;
            showMessage( "Missing contact details", $("#primaryContactMessage") );
         }
         
         form.find( "#firstName" ).val( contact.firstName );
         form.find( "#lastName" ).val( contact.lastName );
         //form.find( "#email" ).val( contact.email );
         
         //Fill in overview information
         if (contact.firstName || contact.lastName) {
            $('#overviewName').text( contact.firstName + " " + contact.lastName );
         } else {
            showMessage( "Missing contact details", $("#overviewMessage") );
         }
         $('#overviewEmail').text( contact.email );
         $('#overviewOrganisation').text( data.organization );

      },
      error : function(response, xhr) {
         showMessage( jQuery.parseJSON( response ).message, $("#primaryContactMessage") )
      }
   } );
} );
