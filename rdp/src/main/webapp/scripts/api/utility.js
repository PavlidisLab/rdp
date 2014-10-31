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
   
   utility.confirmModal = function(callback) {

   }
   
   utility.callback = function(result) {
      
   }
   
   utility.modalAlert = function(callback) {
      var modal = $( "#confirmModal");
      modal.modal({
         backdrop:'static',
         keyboard: false
      });
      
      modal.unbind('click').on('click', 'button', function(e) {
         console.log('click');
         console.log(e);
         modal.modal('hide');
         result = true;
         callback(result);
      });
      
      modal.unbind('hide.bs.modal').on('hide.bs.modal', 'button', function(e) {
         console.log('hide')
         console.log(e);
         result = false;
         callback(result);
      });
      
      return modal;
      
   }
   
   utility.taxonIdToName = {
                 562:'E. Coli',
                 6239:'Roundworm',
                 7227:'Fruit Fly',
                 7955:'Zebrafish',
                 9606:'Human',
                 10090:'Mouse',
                 10116:'Rat',
                 559292:'Yeast'                            
   }
   
   utility.taxonNameToId = {
                            'E. Coli': 562, 
                            'Roundworm': 6239, 
                            'Fruit Fly': 7227, 
                            'Zebrafish': 7955, 
                            'Human': 9606,
                            'Mouse': 10090,
                            'Rat': 10116,
                            'Yeast':559292
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
