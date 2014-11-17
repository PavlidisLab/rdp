/**
 * @memberOf utility
 */
(function( utility, $, undefined ) {
   
   toggleTheme = function() {
      
      var theme_css = $('link.themes');
      theme_css.each(function(i, el ) {
         console.log(el.disabled)
         if ( el.disabled == true ) {
            el.disabled = false;
         } else {
            el.disabled = true;
         }
      });
      
   }
   
   utility.showMessage = function(message, messageEl ) {
      console.log( message );
      messageEl.html( message );
      var parent = messageEl.parent();
         parent.show().addClass("pulse");
         parent.one(
            "webkitAnimationEnd oanimationend msAnimationEnd animationend",
            function() {
                $(this).removeClass("pulse");
            }
        );

   }

   utility.hideMessage = function( messageEl ) {
      messageEl.html( '' );
      messageEl.parent().hide();
   }
   
   utility.isUndefined = function( variable ) {
      return ( typeof variable === 'undefined' );
   }
      
   utility.confirmModal = function(callback) {
      var modal = $( "#confirmModal");
      modal.modal('show');
      
      modal.off('click',  'button.return-choice').on('click', 'button.return-choice', function(e) {
         modal.off('hide.bs.modal');
         modal.modal('hide');
         result = $(this).hasClass("return-true") ? true:false;
         callback(result);
      });
      
      modal.off('hide.bs.modal').on('hide.bs.modal', function(e) {
         result = false;
         callback(result);
      });
      
      return modal;
      
   }
   
   utility.setConfirmChanges = function(tab, isDirty, fixSidebar) {
      tab.one('hide.bs.tab', function(e) {
         if ( isDirty() ) {
            fixSidebar.trigger('click.metisMenu');
            e.preventDefault();
            var relatedTarget = e.relatedTarget;
            utility.confirmModal( function(result) {
               if ( result ) {
                  $(e.relatedTarget).trigger("click");
                  $(e.relatedTarget).tab('show');
               } else {
                  utility.setConfirmChanges(tab, isDirty, fixSidebar);
               }
            });
         }
      });
   }
   
   utility.executeAjax = function(ajax_url, data, verbose) {
      data = utility.isUndefined( data ) ? {} : data;
      verbose = utility.isUndefined( verbose ) ? true : verbose;
      var promise = $.ajax( {
         cache : false,
         url : ajax_url,
         data : data,
         dataType : "json",
         success : function(response, xhr) {
            if (verbose) {console.log(response.message);}
         },
         error : function(response, xhr) {
            if (verbose) {console.log("Error:",response);}
         }
      } );
      
      return promise;
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
