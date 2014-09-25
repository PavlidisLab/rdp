   /**
    *
    * @memberOf listResearchers
    */
(function( listResearchers, $, undefined ) {
   
   var allResearchers;
   var table;
	
	populateResearcherTable = function(researchers, specificGene) {
	   table = $("#listResearchersTable").DataTable();
	    table.clear().draw();
	    $.each(researchers, function(index, researcher) {
	        
	        if ( specificGene ) {
	           for (var i=0;i<researcher.genes.length;i++) {
	              if ( ( researcher.genes[i].officialSymbol == specificGene.officialSymbol ) &&
	                   ( researcher.genes[i].taxon == specificGene.taxon ) ) {
	                 var specificTier = researcher.genes[i].tier;
	              }

	           }
	        }
	         
	
	        var researcherRow = [ researcher.index,
	                              researcher.userName || "", 
	                              researcher.email || "",
	                              researcher.firstName || "", 
	                              researcher.lastName || "",
	                              researcher.organization || "", 
	                              researcher.genes.length || 0,
	                              specificTier || ""]
	        table.row.add(researcherRow).draw();
	    });
	    
	    // Attach listeners to users in table
	    $('a[href^="#displayResearcher"]').click(function (e) {
	          var index = $(this).attr('href').substr(18); // Length of '#displayResearcher'
	          if ( index ==="" ) {
	             // Something went wrong when trying to attach the researchers index to the hyperlink
	             console.log("Cannot find researcher!")
	             return;
	          }
	          index = parseInt(index);
	          tabName = allResearchers[index].userName;
	          
            var nextTab = $('#registerTab li').size()+1;
            
            // create the tab
            $('<li id="tab'+nextTab+'"><a href="#overview" data-toggle="tab"><button class="close" title="Remove this page" type="button"> &times </button>'+tabName+'</a></li>').appendTo('#registerTab');
            
            $('#registerTab li[id="tab'+nextTab+'"]').on('show.bs.tab',function() {
               overview.showOverview( allResearchers[index], true );
               overview.hideButtons();
               });           
            
            // make the new tab active
            $('#registerTab a:last').tab('show');
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
	            var i =0
	            $.each(response, function(index, json) {
	               var r = new researcherModel.Researcher();
	               r.parseResearcherObject( $.parseJSON(json) )
	               r.index = i;
	               researchers.push( r );
	               i++;
	            });
	            console.log("Loaded All Researchers:",researchers);
	            allResearchers = researchers;
	            $('#registerTab a[href="#registeredResearchers"]').show();
	            $("#listResearchersTable").DataTable().column(7).visible(false);
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
	       for (var j=0;j<allResearchers[i].genes.length;j++) {
	          var gene2 = allResearchers[i].genes[j];
             if ( ( gene.officialSymbol == gene2.officialSymbol ) &&
                ( gene.taxon == gene2.taxon ) ) {
                researchers.push(allResearchers[i]);
                continue;
                
             }
	       }
	    }
	    
	    $("#listResearchersTable").DataTable().column(7).visible(true);
	    populateResearcherTable(researchers, gene);

	}

	listResearchers.initDataTable = function() {
	      // Initialize datatable
	      table = $( "#listResearchersTable" ).dataTable( {
	         "aoColumnDefs": [ 
	           {
	              "aTargets": [ 0 ],
	              "defaultContent": "",
	              "visible":false,
	              "searchable":false
	           }],
	         "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
	            // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
	            // whereas aData[0] refers to the data in the first column, hidden or not
	            $('td:eq(0)', nRow).html('<a href="#displayResearcher' + aData[0] + '">'+ aData[1] + '</a>');
	            return nRow;
	         },
	         
	      } );
	   }
	
}( window.listResearchers = window.listResearchers || {}, jQuery ));

$(document).ready(function() {
    // Initialize the taxon combo with options from an existing taxon combo
    $("#taxonCommonNameSelectListResearchers").empty();
    $("#taxonCommonNameSelectListResearchers").append( $("#taxonCommonNameSelect").children().clone() );

    $("#navbarUsername").on("loginSuccess", function(event, response) {
        // Only show Researchers tab if current user is "admin"
        if (jQuery.parseJSON(response).isAdmin == "true") {
         listResearchers.initDataTable();
        	listResearchers.getResearchers();
        	$("#updateCache").click(listResearchers.updateCache)
        	$("#resetResearchersButton").click(listResearchers.getResearchers)
        	$('#registerTab li:first').on('show.bs.tab',function() {
        	   overview.showOverview( researcherModel.currentResearcher );
        	   overview.showButtons();   
        	   });
        	$(".nav-tabs").on("click", "button", function (event) {
        	   event.stopPropagation();
            var anchor = $(this).parent();
            //$(anchor.attr('href')).remove();
            $(this).parent().parent().remove();
            $('#registerTab a[href="#registeredResearchers"]').tab('show')
            //$('#registerTab a:last').tab('show')
        });
        }
    });

    editGenes.initSelect2({
        'container' : $("#findResearchersByGenesSelect"),
        'taxonEl' : $( "#taxonCommonNameSelectListResearchers" )
    })

    $("#findResearchersByGeneButton").click(listResearchers.findResearchersByGene)

});