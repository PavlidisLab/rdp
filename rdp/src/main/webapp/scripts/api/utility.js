/**
 * @memberOf utility
 */
(function( utility, $, undefined ) {
   utility.showMessage = function(message, messageEl ) {
      console.log( message );
      messageEl.html( message );
      messageEl.parent().show();
   }

   utility.hideMessage = function( messageEl ) {
      messageEl.html( '' );
      messageEl.parent().hide();
   }
   
   utility.isUndefined = function( variable ) {
      return ( typeof variable === 'undefined' );
   }
   
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
   
}( window.utility = window.utility || {}, jQuery ));
