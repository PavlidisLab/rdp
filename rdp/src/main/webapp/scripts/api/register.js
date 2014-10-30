// Initialize document
$( document ).ready( function() {
   
   var promise = $.ajax( {
	      cache : false,
	      type : 'GET',
	      url : "ajaxLoginCheck.html",
	      success : function(response, xhr) {
	         //$( "#navbarUsername" ).text( jQuery.parseJSON( response ).user );
	         //$( "#navbarIsAdmin" ).text( jQuery.parseJSON( response ).isAdmin );
	         //$( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	         $( "body" ).trigger( "loginSuccess", response );
	         console.log("successfully logged in as: " + jQuery.parseJSON( response ).user);
	      },
	      error : function(response, xhr) {
	         console.log( xhr.responseText );
	         //$( "#navbarUsername" ).text( "Anonymous" );
	         //$( "#navbarUsername" ).append( ' <span class="caret"></span>' );
	      }
	   } );
         
   promise = researcherModel.loadResearcher();
   
   // This gets run when the ajax calls in defs have completed
   $.when(promise).done(function() {
      //$('.tab-pane a[href="#overview"]').tab('show'); 
      //profile.setInfo();
   });
   
   $('a[href="#modelOrganisms"]').click(function(){
      $("#currentOrganismBreadcrumb").text($(this).text());
      $("#modelOrganisms .main-header em").text ( $(this).text() );
      modelOrganisms.setFocus();
      geneManager.fillTable();
    });
   
   $('#settingsDropdown').click(function(){
      $('#menu a[href="#profile"]').tab('show');
      $('.tab-pane a[href="#settings-tab"]').tab('show');
    });
   
   $('#profileDropdown').click(function(){
      $('#menu a[href="#profile"]').tab('show');
      $('.tab-pane a[href="#profile-tab"]').tab('show');
    });
   
/*   $('a[href="#profile"]').on('show.bs.tab', function() {
      $('a[href="#profile"]').parent('li').removeClass('active');
      $(this).parent('li').addClass('active');
      
   } );*/
   
   
} );
