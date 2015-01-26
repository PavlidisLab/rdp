/**
 * @memberOf profile
 */
(function( profile, $, undefined ) {
   profile.firstName = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .basic-info .data-row').eq( 0 ).find('.data-value');
   };
   profile.lastName = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .basic-info .data-row').eq( 1 ).find('.data-value');
   };
   profile.organization = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .basic-info .data-row').eq( 2 ).find('.data-value');
   };
   profile.department = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .basic-info .data-row').eq( 3 ).find('.data-value');
   };
   profile.website = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .basic-info .data-row').eq( 4 ).find('.data-value');
   };
   
   profile.email = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .contact-info .data-row').eq( 0 ).find('.data-value');
   };
   profile.phone = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .contact-info .data-row').eq( 1 ).find('.data-value');
   };
   profile.description = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' .about p').eq( 0 );
   };

   profile.pubMedIds = function(idOverride) {
      idOverride = utility.isUndefined(idOverride) ? '#profile-tab' : idOverride;
      return $(idOverride+' p.publicationsList').eq( 0 );
   };

   saveProfile = function(e) {
      var btn = $(this);
      btn.attr("disabled", "disabled");
      btn.children('i').addClass('fa-spin');
      lockAll();
      researcherModel.currentResearcher.firstName = profile.firstName().text();
      researcherModel.currentResearcher.lastName = profile.lastName().text();
      researcherModel.currentResearcher.department = profile.department().text();
      researcherModel.currentResearcher.organization = profile.organization().text();
      researcherModel.currentResearcher.website = profile.website().text();
      researcherModel.currentResearcher.phone = profile.phone().text();
      researcherModel.currentResearcher.description = profile.description().text();

/*      researcherModel.currentResearcher.pubMedIds = [];
      console.log(profile.pubMedIds() );
      console.log(profile.pubMedIds().html() );
      profile.pubMedIds().find( 'div' ).each(function(index) {
         var val = parseInt( $(this).text() );
         if ( !isNaN(val) ) {
         researcherModel.currentResearcher.pubMedIds.push( val );
         }
      });*/
      try {
      var ids = [];
      var separators = ['<div>','<br>'];
      var stringIds = profile.pubMedIds().html().split(new RegExp(separators.join('|'), 'g'));
      for (var i=0; i<stringIds.length; i++) {
         var val = parseInt( stringIds[i] );
         if ( !isNaN(val) ) {
            ids.push( val );
         }
      }
   } catch(err) {
      console.log('Could not parse PubMedIds');
   }
      
      researcherModel.currentResearcher.pubMedIds = ids;

      var promise = researcherModel.saveResearcherProfile();
      $.when(promise).done(function() {

         console.log("Saved Changes");
         utility.showMessage( promise.responseJSON.message, $( "#myAccount .alert div" ) );
      }).fail(function() {
         console.log("Failed to Save Changes");
         utility.showMessage( "FAILED to save changes", $( "#myAccount .alert div" ) );
      }).always(function() {
         btn.removeAttr("disabled");
         btn.children('i').removeClass('fa-spin');
      });
         
   }

   profile.isChanged = function() {
      researcher = researcherModel.currentResearcher;
      var changed = false;

      changed |= profile.firstName().text() != researcher.firstName;  
      changed |= profile.lastName().text() != researcher.lastName;     
      changed |= profile.organization().text() != researcher.organization; 
      changed |= profile.department().text() != researcher.department;
      changed |= profile.website().text() != researcher.website;

      changed |= profile.phone().text() != researcher.phone;

      changed |= profile.description().text() != researcher.description;

      try {
      if ( !changed && researcher.pubMedIds.length > 0 ) {
         researcher.pubMedIds.sort(function(a, b){return a-b});
         var ids = [];
         var separators = ['<div>','<br>'];
         var stringIds = profile.pubMedIds().html().split(new RegExp(separators.join('|'), 'g'));
/*         profile.pubMedIds().find( 'div' ).each(function(index) {
            var val = parseInt( $(this).text() );
            if ( !isNaN(val) ) {
               ids.push( val );
            }
         });*/
         for (var i=0; i<stringIds.length; i++) {
            var val = parseInt( stringIds[i] );
            if ( !isNaN(val) ) {
               ids.push( val );
            }
         }
                  
         ids.sort(function(a, b){return a-b});
         if ( ids.length != researcher.pubMedIds.length ) {
            return true;
         }
         for (var i=0; i<ids.length; i++) {
            if ( ids[i] != researcher.pubMedIds[i] ) {
               return true;
            }
         }
      }
      } catch(err) {
         console.log('Could not parse PubMedIds');
      }

      return changed;

   }

   profile.hideTab = function() {
      console.log('hide');
   }

   profile.setInfo = function( researcher, idOverride ) {
      lockAll();
      researcher = utility.isUndefined( researcher ) ? researcherModel.currentResearcher : researcher;
      profile.firstName(idOverride).text(researcher.firstName);
      profile.lastName(idOverride).text(researcher.lastName);
      profile.organization(idOverride).text(researcher.organization);
      profile.department(idOverride).text(researcher.department);

      if ( researcher.website ) {
         profile.website(idOverride).html( "<a href='" + researcher.website + "' target='_blank'>"+ researcher.website + "</a>" );
      } else {
         profile.website(idOverride).html("");
      }

      profile.email(idOverride).text(researcher.email);
      profile.phone(idOverride).text(researcher.phone);
      profile.description(idOverride).text(researcher.description);
      profile.pubMedIds(idOverride).empty();


      if ( researcher.pubMedIds.length > 0 ) {
         researcher.pubMedIds.sort(function(a, b){return a-b});
         //var pubMedHTML = researcher.pubMedIds.join('<br/>');
         for (var i=0; i<researcher.pubMedIds.length; i++) {
            profile.pubMedIds(idOverride).append( '<div>' + researcher.pubMedIds[i] + '</div>'  )
         }
         //profile.pubMedIds(idOverride).html(pubMedHTML);
      } else {
         //profile.pubMedIds(idOverride).append( '<li></li>'  )
      }
      //profile.pubMedIds(idOverride).append( '<div style="height: 1px;">'  )
   }



   editProfile = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-edit').addClass('fa-check-square-o').removeClass('yellow-icon').addClass('green-icon');
      var div = $(this).closest('div');
      
      var a = $(this).closest('a');
      
      a.prop('title', 'Save');

      $('.data-editable', div).each( function(idx) {
         $(this).prop('contenteditable',true);
         $(this).addClass('editable');
      });


   }

   lockProfile = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-check-square-o').addClass('fa-edit').removeClass('green-icon').addClass('yellow-icon');
      var div = $(this).closest('div');
      var a = $(this).closest('a');
      
      a.prop('title', 'Edit');

      $('*[contenteditable="true"]', div).each( function(idx) {
         $(this).removeAttr('contenteditable');
         $(this).removeClass('editable');
      });

   }

   lockAll = function() {
      $('#profile-tab a i[class~="fa-check-square-o"]').trigger( "click" );
   }

   profile.init = function() {
      $('#profile-tab a').on('click', 'i[class~="fa-edit"]', editProfile );
      $('#profile-tab a').on('click', 'i[class~="fa-check-square-o"]', lockProfile );
      $('a[href="#myAccount"]').on('show.bs.tab', function() {
         profile.setInfo(); 
         utility.setConfirmChanges($('a[href="#myAccount"]'), profile.isChanged, $('a[href="#myAccount"]'))

         } ); 
      $('a[href="#myAccount"]').on('hidden.bs.tab', function() {
         utility.hideMessage( $( "#myAccount .alert div" ) );
      });
            
      profile.pubMedIds()[0].addEventListener("paste", function(e) {
         // cancel paste
         e.preventDefault();

         // get text representation of clipboard
         var text = e.clipboardData.getData("text/plain");
         console.log(text);

         // insert text manually
         document.execCommand("insertText", false, text);
     });

      $('#profile-tab button').click(saveProfile);
   }

}( window.profile = window.profile || {}, jQuery ));

$( document ).ready( function() {
   profile.init();
});