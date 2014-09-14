   /**
    * @memberOf editUser
    */
(function( editUser, $, undefined ) {

	editUser.changePassword = function() {
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
	      },
	      error : function(response, xhr) {
	         console.log( xhr.responseText );
	         showMessage( xhr.status, $( "#changePasswordMessage" ) );
	      }
	   } );
	}
	
	editUser.closeModal = function() {
		hideMessage( $( "#changePasswordMessage" ) );
	}
	
	editUser.fillForm = function() {
		hideMessage( $( "#changePasswordMessage" ) );
        var form = $( "#changePasswordForm" );
        form.find( "#email" ).val( researcherModel.getEmail() );
	}

}( window.editUser = window.editUser || {}, jQuery ));

$( document ).ready( function() {
	$( "#btnChangePassword" ).click( editUser.changePassword );
	$( '#changePasswordModal' ).on( 'hidden.bs.modal', editUser.closeModal );
	$( '#changePasswordModal' ).on( 'show.bs.modal', editUser.fillForm );
	
	
});