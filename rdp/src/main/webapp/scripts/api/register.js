// Initialize document
$( document ).ready( function() {
   
   $( '#menu' ).metisMenu({'toggle':false});
   var loadPromise = researcherModel.loadResearcher();
   $('#navbar-username').html('<i class="fa fa-spinner fa-spin"></i>');
   $.ajax( {
	      cache : false,
	      type : 'GET',
	      url : "ajaxLoginCheck.html",
	      success : function(response, xhr) {
	         //$( "#navbarUsername" ).text( jQuery.parseJSON( response ).user );
	         //$( "#navbarIsAdmin" ).text( jQuery.parseJSON( response ).isAdmin );
	         //$( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	         $( "body" ).trigger( "loginSuccess", response );
	         var responseJSON = jQuery.parseJSON( response );
	         console.log("successfully logged in as: " + responseJSON.user);
	         
	         if (responseJSON.isAdmin == 'true'){
	            console.log("Is Admin");
	            $('#menu > li').has('a[href="#admin"][data-toggle="tab"]').show();
	            admin.init();
	         }
	         
	      },
	      error : function(response, xhr) {
	         console.log( xhr.responseText );
	         //$( "#navbarUsername" ).text( "Anonymous" );
	         //$( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	      }
	   } );
   
   window.onbeforeunload = function (e) {
      e = e || window.event;

      var currentTab = $("#menu li.active a").attr('href');
      var dirtyCheck = function() {return false;};
      if (currentTab == "#modelOrganisms") {
         dirtyCheck = modelOrganisms.isChanged;
      } else if ( currentTab == "#myAccount") {
         dirtyCheck = profile.isChanged;
      }

      if ( dirtyCheck() ) {
      
         // For IE and Firefox prior to version 4
         if (e) {
             e.returnValue = 'You have unsaved changes!';
         }
   
         // For Safari
         return 'You have unsaved changes!';
      }
   };
            
   
   
   // This gets run when the ajax calls in defs have completed
   $.when(loadPromise).done(function() {
      //$('a[href="#myAccount"]').trigger('show.bs.tab');
      $('#menu > li').has('a[href="#modelOrganisms"][data-toggle="tab"]').show();
      $('#navbar-username').html(researcherModel.currentResearcher.userName);
      
      // Combination of both taxons from entered genes and from entered terms
      var taxonIds = utility.uniqueSoft( researcherModel.currentResearcher.getTaxons().concat(Object.keys(researcherModel.currentResearcher.terms)) );
      
      $('#myModelOrganismsList ul li').has('a[href="#modelOrganisms"]').each( function(index) {
         var taxonName = $('a',this).text();
         var taxonId = utility.taxonNameToId[ taxonName ];
         if ( !utility.containsSoft(taxonIds, taxonId) ) {
            $(this).hide();
         } else {
            $('#myModelOrganismsList > ul > li > ul > li').has('a:contains("'+taxonName+'")').hide();
         }
      });
      //$('.tab-pane a[href="#overview"]').tab('show'); 
      //profile.setInfo();
   });
   
   $('#myModelOrganismsList').on('click', 'a[href="#add"]',  function() {
      var taxonName = $(this).text().trim();
      $(this).parent('li').hide();
      $('#myModelOrganismsList > ul > li').has('a[href="#modelOrganisms"]:contains("'+taxonName+'")').show().find('a').click();
      if ( $('#myModelOrganismsList > ul > li > ul > li:visible').length == 0 ) {
         $('#myModelOrganismsList > ul > li > ul > li').has('a[href="#"]').show();
      }
   });

   $('#menu').on('click', 'a[href="#modelOrganisms"][data-toggle!="tab"]', function(){
      if ( $('#menu li.active > a').is('a[href="#modelOrganisms"]') && modelOrganisms.isChanged() ) {
         var $this = $(this);
         $('#myModelOrganismsList > ul > li a[href="#modelOrganisms"]:contains("'+geneManager.currentTaxon()+'")').trigger('click.metisMenu');
         var taxonName = $(this).text();
         utility.confirmModal( function(result) {
            if ( result ) {
               utility.hideMessage( $( "#modelOrganisms .main-header .alert div" ) );
               $this.trigger('click.metisMenu');
               modelOrganisms.load( taxonName );
            }
         });
      } else {
         utility.hideMessage( $( "#modelOrganisms .main-header .alert div" ) );
         modelOrganisms.load( $(this).text() );
      }
   });



   $('#menu').on('click', 'a[href="#modelOrganisms"][data-toggle="tab"]', function(){
      return false;
    });
   
/*   $('a[href="#modelOrganisms"]').click(function(){
      $("#currentOrganismBreadcrumb").text($(this).text());
      $("#modelOrganisms .main-header em").text ( $(this).text() );
      modelOrganisms.setFocus();
      geneManager.fillTable();
    });*/
   
   $('#settingsDropdown').click(function(){
      $('#menu a[href="#myAccount"]').tab('show');
      $('#menu a[href="#myAccount"]').trigger('click.metisMenu');
      $('.tab-pane a[href="#settings-tab"]').tab('show');
    });
   
   $('#profileDropdown').click(function(){
      
      $('#menu a[href="#myAccount"]').tab('show');
      $('#menu a[href="#myAccount"]').trigger('click.metisMenu');
      $('.tab-pane a[href="#profile-tab"]').tab('show');
    });
   
   $('a[href="#help"][data-toggle!="tab"]').click(function(){
      
      $('#menu a[href="#help"]').tab('show');
      $('#menu a[href="#help"]').trigger('click.metisMenu');
    });
   
   $('a[href="#support"][data-toggle!="tab"]').click(function(){
      
      $('#menu a[href="#support"]').tab('show');
      $('#menu a[href="#support"]').trigger('click.metisMenu');
    });
   
   $('#sidebar-brand-bottom').click(function(){
      window.open("http://www.rare-diseases-catalyst-network.ca/",'_blank');
    });
   
/*   $('a[href="#profile"]').on('show.bs.tab', function() {
      $('a[href="#profile"]').parent('li').removeClass('active');
      $(this).parent('li').addClass('active');
      
   } );*/
   
   
} );
