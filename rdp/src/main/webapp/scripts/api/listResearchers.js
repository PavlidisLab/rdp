   /**
    *
    * @memberOf listResearchers
    */
(function( listResearchers, $, undefined ) {
   
   var allResearchers;
	
	populateResearcherTable = function(researchers, specificGene) {
	   table = $("#listResearchersTable").DataTable();
	    table.clear().draw();
	    $.each(researchers, function(i, item) {
	        if (!item.contact) {
	            console.log("Researcher id " + item.id + " has no contact info");
	            return;
	        }
	        
	        item.allGenes = [];
	        for (var tier in item.genes) {
	           if (item.genes.hasOwnProperty(tier)) {
	              for (var i=0;i<item.genes[tier].length;i++) {
	                 item.genes[tier][i].tier = tier;
	                 item.allGenes.push(item.genes[tier][i]);
	              }
	           }
	        }
	        
	        if ( specificGene ) {
	           for (var i=0;i<item.allGenes.length;i++) {
	              if ( ( item.allGenes[i].officialSymbol == specificGene.officialSymbol ) &&
	                   ( item.allGenes[i].taxon == specificGene.taxon ) ) {
	                 var specificTier = item.allGenes[i].tier;
	              }

	           }
	        }
	         
	
	        var researcherRow = [ item.contact.userName || "", 
	                              item.contact.email || "",
	                              item.contact.firstName || "", 
	                              item.contact.lastName || "",
	                              item.organization || "", 
	                              item.allGenes.length || 0,
	                              specificTier || ""]
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
	
	
	listResearchers.getResearchers = function() {
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
	            console.log("Loaded All Researchers:",researchers);
	            allResearchers = researchers;
	            $('#registerTab a[href="#registeredResearchers"]').show();
	            $("#listResearchersTable").DataTable().column(6).visible(false);
	            $( "#findResearchersByGenesSelect" ).select2("val", "");
	            populateResearcherTable(researchers);
	
	            
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
	    
	    researchers = [];
	    
	    for (var i=0;i<allResearchers.length;i++) {
	       for (var j=0;j<allResearchers[i].allGenes.length;j++) {
	          var gene2 = allResearchers[i].allGenes[j];
             if ( ( gene.officialSymbol == gene2.officialSymbol ) &&
                ( gene.taxon == gene2.taxon ) ) {
                researchers.push(allResearchers[i]);
                continue;
                
             }
	       }
	    }
	    
	    $("#listResearchersTable").DataTable().column(6).visible(true);
	    populateResearcherTable(researchers, gene);

	}

}( window.listResearchers = window.listResearchers || {}, jQuery ));

$(document).ready(function() {
    // Initialize the taxon combo with options from an existing taxon combo
    $("#taxonCommonNameSelectListResearchers").empty();
    $("#taxonCommonNameSelectListResearchers").append( $("#taxonCommonNameSelect").children().clone() );

    $("#navbarUsername").on("loginSuccess", function(event, response) {
        // Only show Researchers tab if current user is "admin"
        if (jQuery.parseJSON(response).isAdmin == "true") {
        	listResearchers.getResearchers();
        	$("#updateCache").click(listResearchers.updateCache)
        	$("#resetResearchersButton").click(listResearchers.getResearchers)
        }
    });

    editGenes.initSelect2({
        'container' : $("#findResearchersByGenesSelect"),
        'taxonEl' : $( "#taxonCommonNameSelectListResearchers" )
    })

    $("#findResearchersByGeneButton").click(listResearchers.findResearchersByGene)

});