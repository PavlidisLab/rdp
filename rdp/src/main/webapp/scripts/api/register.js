// Initialize document
$( document ).ready( function() {
   
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
