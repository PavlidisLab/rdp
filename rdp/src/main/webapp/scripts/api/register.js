// Initialize document
$( document ).ready( function() {
   
   $( function() {

      $( '#menu' ).metisMenu({'toggle':false});
   } );
   
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
            
   var promise = researcherModel.loadResearcher();
   
   // This gets run when the ajax calls in defs have completed
   $.when(promise).done(function() {
      $('#navbar-username').html(researcherModel.currentResearcher.userName)
      var taxonIds = researcherModel.currentResearcher.getTaxons();
      $('#myModelOrganismsList ul li').has('a[href="#modelOrganisms"]').each( function(index) {
         var taxonName = $('a',this).text()
         var taxonId = utility.taxonNameToId[ taxonName ];
         if ( taxonIds.indexOf( taxonId ) == -1 ) {
            $(this).hide();
         } else {
            $('#myModelOrganismsList > ul > li > ul > li').has('a:contains("'+taxonName+'")').hide();
         }
      });
      //$('.tab-pane a[href="#overview"]').tab('show'); 
      //profile.setInfo();
   });
   
   $('#myModelOrganismsList').on('click', 'a[href="#add"]',  function() {
      var taxonName = $(this).text().split('Add ').pop().trim();
      $(this).parent('li').hide();
      $('#myModelOrganismsList > ul > li').has('a[href="#modelOrganisms"]:contains("'+taxonName+'")').show().find('a').click();
      if ( $('#myModelOrganismsList > ul > li > ul > li:visible').length == 0 ) {
         $('#myModelOrganismsList > ul > li > ul > li').has('a[href="#"]').show();
      }
   });

   $('#menu').on('click', 'a[href="#modelOrganisms"][data-toggle!="tab"]', function(){
      if ( $('#menu li.active > a').is('a[href="#modelOrganisms"]') && modelOrganisms.isChanged() ) {
         var $this = $(this);
         $('#myModelOrganismsList > ul > li a[href="#modelOrganisms"]:contains("'+geneManager.currentTaxon()+'")').trigger('click.metisMenu')
         var taxonName = $(this).text();
         utility.confirmModal( function(result) {
            if ( result ) {
               $this.trigger('click.metisMenu');
               modelOrganisms.load( taxonName );
            }
         });
      } else {
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
      $('#menu a[href="#profile"]').tab('show');
      $('#menu a[href="#profile"]').trigger('click.metisMenu');
      $('.tab-pane a[href="#settings-tab"]').tab('show');
    });
   
   $('#profileDropdown').click(function(){
      
      $('#menu a[href="#profile"]').tab('show');
      $('#menu a[href="#profile"]').trigger('click.metisMenu');
      $('.tab-pane a[href="#profile-tab"]').tab('show');
    });
   
/*   $('a[href="#profile"]').on('show.bs.tab', function() {
      $('a[href="#profile"]').parent('li').removeClass('active');
      $(this).parent('li').addClass('active');
      
   } );*/
   
   
} );
