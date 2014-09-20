   /**
    * FIXME Needs to be looked at
    * @memberOf listResearchers
    */
(function( listResearchers, $, undefined ) {
	
	listResearchers.jsonToResearcherTable = function(response, table) {
	    table.clear().draw();
	    $.each(response, function(i, item) {
	        if (!item.contact) {
	            console.log("Researcher id " + item.id + " has no contact info");
	            return;
	        }
	
	        var researcherRow = [ item.contact.userName || "", 
	                              item.contact.email || "",
	                              item.contact.firstName || "", 
	                              item.contact.lastName || "",
	                              item.organization || "", 
	                              item.genes.length || 0]
	        table.row.add(researcherRow).draw();
	    });
	}
	listResearchers.updateCache = function() {
	   
      var promise = $.ajax( {
         url : "updateCache.html",
         success : function(response, xhr) {
            response = jQuery.parseJSON(response);
            console.log(response.message);
         },
         error : function(response, xhr) {
           console.log("Error:",response);
         }
      } );
      
      return promise;
	}
	
	
	listResearchers.showResearchers = function() {
	   hideMessage($("#listResearchersMessage"));
	    $.ajax({
	        cache : false,
	        type : 'GET',
	        url : "loadAllResearchers.html",
	        success : function(response, xhr) {
	            // get this from a controller
	            // var response = '[{"userName":"testUsername2",
	            // "email":"testEmail2", "firstName":"testFirstname2",
	            // "lastName":"testLastname2", "organization":"testOrganization2",
	            // "department":"testDepartment2" }]';
	            response = $.parseJSON(response);
	            var researchers = [];
	            $.each(response, function(index, json) {
	               researchers.push( $.parseJSON(json) );
	            });
	            console.log(researchers);
	            $('#registerTab a[href="#registeredResearchers"]').show();
	            listResearchers.jsonToResearcherTable(researchers, $("#listResearchersTable").DataTable());
	
	            
	        },
	        error : function(response, xhr) {
	            console.log(xhr.responseText);
	            $("#listResearchersMessage").html(
	                    "Error with request. Status is: " + xhr.status + ". "
	                            + jQuery.parseJSON(response).message);
	            $("#listResearchersFailed").show();
	        }
	    });
	
	}
	
	listResearchers.findResearchersByGene = function() {

	    var gene = $("#findResearchersByGenesSelect").select2("data")
	
	    if (gene == null) {
	        showMessage("Please select a gene", $("#listResearchersMessage"));
	        return;
	    } else {
	       hideMessage( $("#listResearchersMessage") );
	    }
	
	    $.ajax({
	        url : "findResearchersByGene.html",
	        data : {
	            gene : [$.toJSON(gene)],
	            // gene : '{ "ACOX2:Human" :
	            // {"ensemblId":"ENSG00000168306","ncbiGeneId":"8309","officialSymbol":"ACOX2","officialName":"acyl-CoA
	            // oxidase
	            // 2, branched
	            // chain","aliases":[{"alias":"THCCox"},{"alias":"BRCACOX"},{"alias":"BRCOX"}],"text":"<b>ACOX2</b>
	            // (THCCox,BRCACOX,BRCOX) acyl-CoA oxidase 2, branched chain"}}',
	            taxonCommonName : $("#taxonCommonNameSelectListResearchers").val(),
	        },
	        dataType : "json",
	        success : function(response, xhr) {
	            // get this from a controller
	            // var response = '[{"userName":"testUsername2",
	            // "email":"testEmail2", "firstName":"testFirstname2",
	            // "lastName":"testLastname2", "organization":"testOrganization2",
	            // "department":"testDepartment2" }]';
	
	            showMessage(response.message, $("#listResearchersMessage"));
	
               var data = $.parseJSON(response.data);
               var researchers = [];
               $.each(data, function(index, json) {
                  researchers.push( $.parseJSON(json) );
               });
	            
	            listResearchers.jsonToResearcherTable(researchers, $("#listResearchersTable").DataTable());

	        },
	        error : function(response, xhr) {
	            showMessage(response.message, $("#listResearchersMessage"));
	        }
	    });
	}

}( window.listResearchers = window.listResearchers || {}, jQuery ));

$(document).ready(function() {
    // Initialize the taxon combo with options from an existing taxon combo
    $("#taxonCommonNameSelectListResearchers").empty();
    $("#taxonCommonNameSelectListResearchers").append( $("#taxonCommonNameSelect").children().clone() );

    $("#navbarUsername").on("loginSuccess", function(event, response) {
        // Only show Researchers tab if current user is "admin"
        if (jQuery.parseJSON(response).isAdmin == "true") {
        	listResearchers.showResearchers();
        	$("#updateCache").click(listResearchers.updateCache)
        	$("#resetResearchersButton").click(listResearchers.showResearchers)
        }
    });

    editGenes.initSelect2({
        'container' : $("#findResearchersByGenesSelect"),
        'taxonEl' : $( "#taxonCommonNameSelectListResearchers" )
    })

    $("#findResearchersByGeneButton").click(listResearchers.findResearchersByGene)

});