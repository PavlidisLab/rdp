   /**
    * @memberOf editProfile
    */
(function( editProfile, $, undefined ) {

	editProfile.closeProfileModal = function() {
	   hideMessage( $( "#primaryContactMessage" ) );
	   
	};
	
	editProfile.saveResearcher = function() {
        var form = $( "#primaryContactForm" );
        researcherModel.setFirstName( form.find( "#firstName" ).val() );
        researcherModel.setLastName( form.find( "#lastName" ).val());
        researcherModel.setDepartment( form.find( "#department" ).val());
        researcherModel.setOrganization( form.find( "#organization" ).val());
        researcherModel.setWebsite( form.find( "#website" ).val());
        researcherModel.setPhone( form.find( "#phone" ).val());
        researcherModel.setDescription( form.find( "#description" ).val());
        var promise = researcherModel.saveResearcherProfile();
        overview.showProfile();
        $.when(promise).done(function() {
        	showMessage( promise.responseJSON.message, $( "#primaryContactMessage" ) )
        });
	}
	
	editProfile.fillForm = function() {
		hideMessage( $( "#primaryContactMessage" ) );
        var form = $( "#primaryContactForm" );
        form.find( "#firstName" ).val( researcherModel.getFirstName() );
        form.find( "#lastName" ).val( researcherModel.getLastName() );
        form.find( "#department" ).val( researcherModel.getDepartment() );
        form.find( "#organization" ).val( researcherModel.getOrganization() );
        form.find( "#website" ).val( researcherModel.getWebsite() );
        form.find( "#phone" ).val( researcherModel.getPhone() );
        form.find( "#description" ).val( researcherModel.getDescription() );
	}
	

}( window.editProfile = window.editProfile || {}, jQuery ));

$( document ).ready( function() {
	$( "#submit" ).click( editProfile.saveResearcher );
	$( '#editProfileModal' ).on( 'hidden.bs.modal', editProfile.closeProfileModal );
	$( '#editProfileModal' ).on( 'show.bs.modal', editProfile.fillForm );
	
	
});