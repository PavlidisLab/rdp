   /**
    * @memberOf editUser
    */
(function( editUser, $, undefined ) {

	editUser.changePassword = function(event) {
	   event.preventDefault();
	   $.ajax( {
	      cache : false,
	      type : 'POST',
	      url : "editUser.html",
	      beforeSend : function(xhr) {
	         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
	      },
	      data : $( "#changePasswordForm" ).serialize(),
	      success : function(response, xhr) {
	    	  showMessage( jQuery.parseJSON( response ).message, $( "#changePasswordMessage" ) );
	    	  if (!jQuery.parseJSON( response ).success) {
	    	     var form = $( "#changePasswordForm" );
              form.find( "#oldPassword" ).val( "" );
              form.find( "#password" ).val( "" );
              form.find( "#passwordConfirm" ).val( "" );
	    	  }
	      },
	      error : function(response, xhr) {
	         //console.log( xhr.responseText );
	         showMessage( jQuery.parseJSON( response ).message, $( "#changePasswordMessage" ) );
	      }
	   } );
	}
	
	editUser.closeModal = function() {
		hideMessage( $( "#changePasswordMessage" ) );
      var form = $( "#changePasswordForm" );
      form.find( "#oldPassword" ).val( "" );
      form.find( "#password" ).val( "" );
      form.find( "#passwordConfirm" ).val( "" );
	}
	
	editUser.fillForm = function() {
		hideMessage( $( "#changePasswordMessage" ) );
	}

}( window.editUser = window.editUser || {}, jQuery ));

$( document ).ready( function() {
	$( "#changePasswordModal" ).submit( editUser.changePassword );
	$( '#changePasswordModal' ).on( 'hidden.bs.modal', editUser.closeModal );
	$( '#changePasswordModal' ).on( 'show.bs.modal', editUser.fillForm );
	
	
});