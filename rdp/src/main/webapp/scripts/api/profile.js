/**
 * @memberOf profile
 */
(function( profile, $, undefined ) {
   profile.firstName = function() {
      return $('#profile-tab .basic-info .data-row').eq( 0 ).find('.data-value');
   };
   profile.lastName = function() {
      return $('#profile-tab .basic-info .data-row').eq( 1 ).find('.data-value');
   }
   profile.organization = function() {
      return $('#profile-tab .basic-info .data-row').eq( 2 ).find('.data-value');
   }
   profile.department = function() {
      return $('#profile-tab .basic-info .data-row').eq( 3 ).find('.data-value');
   }
   profile.website = function() {
      return $('#profile-tab .basic-info .data-row').eq( 4 ).find('.data-value');
   }

   profile.email = function() {
      return $('#profile-tab .contact-info .data-row').eq( 0 ).find('.data-value');
   }
   profile.phone = function() {
      return $('#profile-tab .contact-info .data-row').eq( 1 ).find('.data-value');
   }
   profile.description = function() {
      return $('#profile-tab .about p').eq( 0 );
   }

   profile.pubMedIds = function() {
      return $('#publicationsList ul').eq( 0 );
   }

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

      researcherModel.currentResearcher.pubMedIds = [];
      profile.pubMedIds().find( 'li' ).each(function(index) {
         var val = parseInt( $(this).text() );
         if ( !isNaN(val) ) {
         researcherModel.currentResearcher.pubMedIds.push( val );
         }
      });

      var promise = researcherModel.saveResearcherProfile();

      $.when(promise).done(function() {
         btn.removeAttr("disabled");
         btn.children('i').removeClass('fa-spin');
         console.log("Saved Changes");
         //utility.showMessage( promise.responseJSON.message, $( "#primaryContactMessage" ) );
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


      if ( !changed && researcher.pubMedIds.length > 0 ) {
         researcher.pubMedIds.sort(function(a, b){return a-b});
         var ids = [];
         profile.pubMedIds().find( 'li' ).each(function(index) {
            var val = parseInt( $(this).text() );
            if ( !isNaN(val) ) {
               ids.push( val );
            }
         });
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

      return changed;

   }

   profile.hideTab = function() {
      console.log('hide');
   }

   profile.setInfo = function( researcher ) {
      lockAll();
      researcher = utility.isUndefined( researcher ) ? researcherModel.currentResearcher : researcher;
      profile.firstName().text(researcher.firstName);
      profile.lastName().text(researcher.lastName);
      profile.organization().text(researcher.organization);
      profile.department().text(researcher.department);

      if ( researcher.website ) {
         profile.website().html( "<a href='" + researcher.website + "' target='_blank'>"+ researcher.website + "</a>" );
      } else {
         profile.website().html("");
      }

      profile.email().text(researcher.email);
      profile.phone().text(researcher.phone);
      profile.description().text(researcher.description);
      profile.pubMedIds().empty();


      if ( researcher.pubMedIds.length > 0 ) {
         researcher.pubMedIds.sort(function(a, b){return a-b});
         for (var i=0; i<researcher.pubMedIds.length; i++) {
            profile.pubMedIds().append( '<li>' + researcher.pubMedIds[i] + '</li>'  )
         }
      }

   }



   editProfile = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-edit').addClass('fa-check-square-o').removeClass('yellow-icon').addClass('green-icon');
      var div = $(this).closest('div');

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
      $('a[href="#profile"]').on('show.bs.tab', function() {
         profile.setInfo(); 
         utility.setConfirmChanges($('a[href="#profile"]'), profile.isChanged, $('a[href="#profile"]'))

         } );     


      $('#profile-tab button').click(saveProfile);
   }

}( window.profile = window.profile || {}, jQuery ));

$( document ).ready( function() {
   profile.init();
});