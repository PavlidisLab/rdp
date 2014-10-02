/**
 * @memberOf resetPassword
 */
(function( resetPassword, $, undefined ) {

   resetPassword.newPassword = function(e) {
      $.ajax( {
         cache : false,
         type : 'POST',
         url : "newPassword.html",
         beforeSend : function(xhr) {
            xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
         },
         data : $( "#resetPasswordForm" ).serialize(),
         success : function(response, xhr) {
            utility.showMessage( $("#resetPasswordMessage") , jQuery.parseJSON( response ).message );
         },
         error : function(response, xhr) {
            console.log( xhr.responseText );
            utility.showMessage( $("#resetPasswordMessage"), "Error with request. Status is: " + xhr.status );
         }
      } );
      e.preventDefault();
      return false;
   };
   
}( window.resetPassword = window.resetPassword || {}, jQuery ));

var urlParams;
(window.onpopstate = function () {
    var match,
        pl     = /\+/g,  // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
        query  = window.location.search.substring(1);

    urlParams = {};
    while (match = search.exec(query))
       urlParams[decode(match[1])] = decode(match[2]);
})();

$( document ).ready( function() {

   $( '#resetPasswordModal' ).modal( {
      keyboard : false,
      backdrop : 'static',
      show : true
   } );
   
   var form = $( "#resetPasswordForm" );
   
   form.submit( resetPassword.newPassword );
 
   form.find( "#user" ).val( urlParams['user'] );
   form.find( "#key" ).val( urlParams['key'] );   


});