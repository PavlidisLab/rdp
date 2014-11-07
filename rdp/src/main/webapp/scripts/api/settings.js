   /**
    * @memberOf settings
    */
(function( settings, $, undefined ) {
   
   settings.saveButton = function() {
      return $('#settings-tab button');
   };
   
   settings.passwordForm = function() {
      return $('#settings-tab form');
   };
   
   changePassword = function(event) {
      event.preventDefault();
      var btn = settings.saveButton();
      btn.attr("disabled", "disabled");
      btn.children('i').addClass('fa-spin');
      $.ajax( {
         cache : false,
         type : 'POST',
         url : "editUser.html",
         beforeSend : function(xhr) {
            xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
         },
         data : settings.passwordForm().serialize(),
         success : function(response, xhr) {
            btn.removeAttr("disabled");
            btn.children('i').removeClass('fa-spin');
            //utility.showMessage( jQuery.parseJSON( response ).message, $( "#changePasswordMessage" ) );
            console.log(jQuery.parseJSON( response ).message);

            var form = settings.passwordForm();
            form.find( "#oldPassword" ).val( "" );
            form.find( "#password" ).val( "" );
            form.find( "#passwordConfirm" ).val( "" );
         },
         error : function(response, xhr) {
            btn.removeAttr("disabled");
            btn.children('i').removeClass('fa-spin');
            //console.log( xhr.responseText );
            //utility.showMessage( jQuery.parseJSON( response ).message, $( "#changePasswordMessage" ) );
         }
      } );
   }
   
   settings.init = function() {
      settings.passwordForm().submit( changePassword );
      settings.saveButton().click(function() {
         settings.passwordForm().submit();
      });
      
   }
   
}( window.settings = window.settings || {}, jQuery ));

$( document ).ready( function() {
   settings.init();
});