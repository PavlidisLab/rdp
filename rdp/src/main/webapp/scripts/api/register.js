var showMessage = function( message, messageEl ) {
   console.log( message );
   messageEl.html( message );
   messageEl.parent().show();
}

var hideMessage = function( messageEl ) {
   messageEl.html( '' );
   messageEl.parent().hide();
}

// Call the button widget method on the login button to format it.
var saveContact = function() {

   console.log( $( "#primaryContactForm" ).serialize() );

   $.ajax( {
      cache : false,
      type : 'POST',
      url : 'saveResearcher.html',
      data : $( "#primaryContactForm" ).serialize(),
      success : function(response, xhr) {
         // console.log( "saved researcher successfully" );
         // $( "#primaryContactMessage" ).html( jQuery.parseJSON( response ).message );
         // $( "#primaryContactFailed" ).show();
         
         loadResearcher();
         
         if ( !response.success ) {
            console.log( jQuery.parseJSON( response ).error );
         }
         
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

var loadResearcher = function() {
   
   $.ajax( {
      // cache : false,
      // type : 'GET',
      url : "loadResearcher.html",
      success : function(response, xhr) {

         var data = jQuery.parseJSON( response ).data;
         var form = $( "#primaryContactForm" );
         console.log(data);
         form.find( "#department" ).val( data.department );
         form.find( "#organization" ).val( data.organization );
         form.find( "#website" ).val( data.website );
         form.find( "#phone" ).val( data.phone );
         form.find( "#description" ).val( data.description );
         
         var contact;
         
         // Researcher created so Researcher object was returned
         if ( data.contact != null ) {
            contact = data.contact;
            hideMessage( $( "#overviewMessage" ) );
         } else {
          // Researcher not created yet so User object was returned, this should not happen...
            contact = data;
            showMessage( "Missing contact details", $("#overviewMessage") );
         }
         
         form.find( "#firstName" ).val( contact.firstName );
         form.find( "#lastName" ).val( contact.lastName );
         //form.find( "#email" ).val( contact.email );
         
         //Fill in overview information
         if (contact.firstName || contact.lastName) {
            $('#overviewName').text( contact.firstName + " " + contact.lastName );
            hideMessage( $( "#overviewMessage" ) );
         } else {
            showMessage( "<a href='#editProfileModal' class='alert-link' data-toggle='modal'>Missing contact details - Click Here</a>", $("#overviewMessage") );
         }
         $('#overviewEmail').text( contact.email );
         $('#overviewOrganisation').text( data.organization );
         if ( data.website ) {
            $('#overviewURL').html( "<a href='" + data.website + "' target='_blank'>"+ data.website + "</a>" );
         }
         $('#overviewFocus').text( data.description );
         
      },
      error : function(response, xhr) {
         showMessage( jQuery.parseJSON( response ).message, $("#overviewMessage") )
      }
   } );
   
   
};

var closeProfileModal = function() {
   hideMessage( $( "#primaryContactMessage" ) );
   
};

// Initialize document
$( document ).ready( function() {
   /*
   $( "#primaryContactForm" ).find( "#submit" ).click( saveContact );
   
   loadResearcher();
   
   $( '#editProfileModal' ).on( 'hidden.bs.modal', closeProfileModal );
   */
   $(function(){
      $("[data-hide]").on("click", function(){
         $(this).closest("." + $(this).attr("data-hide")).hide();
          /*
           * The snippet above will hide the closest element with the class specified in data-hide,
           * i.e: data-hide="alert" will hide the closest element with the alert property.
           *
           * (From jquery doc: For each element in the set, get the first element that matches the selector by
           * testing the element itself and traversing up through its ancestors in the DOM tree.)
          */
      });
   });
   
   var promise = $.ajax( {
	      cache : false,
	      type : 'GET',
	      url : "ajaxLoginCheck.html",
	      success : function(response, xhr) {
	         $( "#navbarUsername" ).text( jQuery.parseJSON( response ).user );
	         $( "#navbarIsAdmin" ).text( jQuery.parseJSON( response ).isAdmin );
	         $( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	         $( "#navbarUsername" ).trigger( "loginSuccess", response );
	         console.log("successfully logged in as: " + jQuery.parseJSON( response ).user);
	      },
	      error : function(response, xhr) {
	         console.log( xhr.responseText );
	         $( "#navbarUsername" ).text( "Anonymous" );
	         $( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	      }
	   } );
         
   promise = researcherModel.loadResearcher();
   
   // This gets run when the ajax calls in defs have completed
   $.when(promise).done(function() {
	   overview.showOverview();   
   });
   
} );
