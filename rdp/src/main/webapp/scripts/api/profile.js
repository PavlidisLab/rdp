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
      return $('#profile-tab .about div p');
   }

   saveProfile = function(e) {
      var btn = $(this);
      btn.attr("disabled", "disabled");
      lockAll();
      researcherModel.currentResearcher.firstName = profile.firstName().text();
      researcherModel.currentResearcher.lastName = profile.lastName().text();
      researcherModel.currentResearcher.department = profile.department().text();
      researcherModel.currentResearcher.organization = profile.organization().text();
      researcherModel.currentResearcher.website = profile.website().text();
      researcherModel.currentResearcher.phone = profile.phone().text();
      researcherModel.currentResearcher.description = profile.description().text();

      var promise = researcherModel.saveResearcherProfile();

      $.when(promise).done(function() {
         btn.removeAttr("disabled");
         console.log("Saved Changes");
         //utility.showMessage( promise.responseJSON.message, $( "#primaryContactMessage" ) );
      });
   }

   profile.setInfo = function( researcher ) {
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

   }



   editProfile = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-edit').addClass('fa-check-square-o').addClass('green-icon');
      var div = $(this).closest('div');
      var newElem = $('<input type="text">');
      $('span[class^="data-value"]', div).each( function(idx) {
         if ( $(this).hasClass("link") ) {
            newElem.addClass('link');
         }
         newElem.val( $(this).text() );
         $(this).replaceWith( newElem.clone() );

      });

      newElem = $('<textarea rows="3" maxlength="1200"></textarea>');
      $('p[class="data-paragraph"]', div).each( function(idx) {
         newElem.text( $(this).text() );
         $(this).replaceWith( newElem.clone() );

      });



   }

   lockProfile = function(e) {
      e.stopPropagation();
      e.preventDefault();
      $(this).removeClass('fa-check-square-o').addClass('fa-edit').removeClass('green-icon');

      var div = $(this).closest('div');
      var newElem = $('<span class="data-value"></span>');
      $('input', div).each( function(idx) {
         if ( $(this).hasClass("link") ) {
            var url = $(this).val()
            if ( url ) {
               url = url.indexOf("http://") === 0 ? url : "http://" + url;
            }
            newElem.html( '<a href="' + url + '">' + url + '</a>');
            newElem.addClass('link');
         } else {
            newElem.text( $(this).val() );
         }

         $(this).replaceWith( newElem.clone() );

      });

      newElem = $('<p class="data-paragraph"></p>');
      $('textarea', div).each( function(idx) {
         newElem.text( $(this).val() );
         $(this).replaceWith( newElem.clone() );

      });

   }

   lockAll = function() {
      $('#profile-tab a i[class~="fa-check-square-o"]').trigger( "click" );
   }

   profile.init = function() {
      $('#profile-tab a').on('click', 'i[class~="fa-edit"]', editProfile );
      $('#profile-tab a').on('click', 'i[class~="fa-check-square-o"]', lockProfile );
      $('a[href="#profile"]').on('show.bs.tab', function() {profile.setInfo() } );
      $('#profile-tab button').click(saveProfile);
   }

}( window.profile = window.profile || {}, jQuery ));

$( document ).ready( function() {
   profile.init();
});