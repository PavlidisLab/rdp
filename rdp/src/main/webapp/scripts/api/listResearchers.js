var jsonToResearcherTable = function(response, tableId) {
   $.each( response, function(i, item) {
      if ( !item.contact ) {
         console.log( "Researcher id " + item.id + " has no contact info" );
         return;
      }
      $( '<tr>' ).append( $( '<td>' ).text( item.contact.userName ), $( '<td>' ).text( item.contact.email ),
         $( '<td>' ).text( item.contact.firstName ), $( '<td>' ).text( item.contact.lastName ),
         $( '<td>' ).text( item.organization ), $( '<td>' ).text( item.department ) ).appendTo( tableId );
      // console.log($tr.wrap('<p>').html());
   } );
};

var showResearchers = function() {

   $.ajax( {
      cache : false,
      type : 'GET',
      url : "loadAllResearchers.html",
      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         response = $.parseJSON( response );
         var tableId = "#listResearchersTable";
         jsonToResearcherTable( response, tableId );

         $( "#listResearchersTable" ).dataTable();
         $( '#registerTab a[href="#registeredResearchers"]' ).show();
      },
      error : function(response, xhr) {
         console.log( xhr.responseText );
         $( "#listResearchersMessage" ).html(
            "Error with request. Status is: " + xhr.status + ". " + jQuery.parseJSON( response ).message );
         $( "#listResearchersFailed" ).show();
      }
   } );

};

var findResearchersByGene = function() {
   

   var gene = $( "#findResearchersByGenesSelect" ).select2( "data" )
   
   console.log("findResearchersByGene gene = " + gene );  
   
   /*
   $.ajax( {
      url : "findResearchersByGene.html",
      data : {
         gene : $.toJSON(gene),
         taxonCommonName : $( "#taxonCommonNameSelect" ).val(),
      },
      dataType : "json",
      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';
         response = $.parseJSON( response );
         var tableId = "#listResearchersTable";
         jsonToResearcherTable( response, tableId );

         $( "#listResearchersTable" ).dataTable();
      },
      error : function(response, xhr) {
         showMessage( response.message,$( "#listResearchersMessage" ));
      }
   } );
   */
}

$( document ).ready( function() {

   $( "#navbarUsername" ).on( "loginSuccess", function(event, response) {
      // Only show Researchers tab if current user is "admin"
      if ( jQuery.parseJSON( response ).isAdmin == "true" ) {
         showResearchers();
      }
   } );

   searchGenes.init( {
      'container' : $( "#findResearchersByGenesSelect" )
   } )
   
   $("#findResearchersByGeneButton").click( findResearchersByGene )

} );