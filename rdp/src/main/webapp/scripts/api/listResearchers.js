var jsonToResearcherTable = function(response, tableId) {
   $( tableId ).DataTable().clear().draw();
   $.each( response, function(i, item) {
      if ( !item.contact ) {
         console.log( "Researcher id " + item.id + " has no contact info" );
         return;
      }

      var researcherRow = [ item.contact.userName, item.contact.email, item.contact.firstName, item.contact.lastName,
                           item.organization, item.department ]
      $( tableId ).DataTable().row.add( researcherRow ).draw();
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

   if ( gene == null ) {
      showMessage( "Please select a gene", $( "#listResearchersMessage" ) );
      return;
   } else {
      $( "#listResearchersFailed" ).hide();
   }

   $.ajax( {
      url : "findResearchersByGene.html",
      data : {
         gene : '{ "' + gene.officialSymbol + '" : ' + $.toJSON( gene ) + '}',
         // gene : '{ "ACOX2" :
         // {"ensemblId":"ENSG00000168306","ncbiGeneId":"8309","officialSymbol":"ACOX2","officialName":"acyl-CoA oxidase
         // 2, branched chain","aliases":[{"alias":"THCCox"},{"alias":"BRCACOX"},{"alias":"BRCOX"}],"text":"<b>ACOX2</b>
         // (THCCox,BRCACOX,BRCOX) acyl-CoA oxidase 2, branched chain"}}',
         taxonCommonName : $( "#taxonCommonNameSelect" ).val(),
      },
      dataType : "json",
      success : function(response, xhr) {
         // get this from a controller
         // var response = '[{"userName":"testUsername2", "email":"testEmail2", "firstName":"testFirstname2",
         // "lastName":"testLastname2", "organization":"testOrganization2", "department":"testDepartment2" }]';

         showMessage( response.message, $( "#listResearchersMessage" ) );

         var tableId = "#listResearchersTable";
         jsonToResearcherTable( $.parseJSON( response.data ), tableId );

         $( "#listResearchersTable" ).dataTable();
      },
      error : function(response, xhr) {
         showMessage( response.message, $( "#listResearchersMessage" ) );
      }
   } );
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

   $( "#findResearchersByGeneButton" ).click( findResearchersByGene )

} );