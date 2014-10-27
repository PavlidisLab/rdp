   /**
    *
    * @memberOf listResearchers
    */
(function( listResearchers, $, undefined ) {
   
   var allResearchers;
   var table;
	
	populateResearcherTable = function(researchers, tiers) {
	   table = $("#listResearchersTable").DataTable();
	    table.clear().draw();
	    $.each(researchers, function(index, researcher) {
	        
	        if ( tiers ) {
	           var specificTier = tiers[index];
//	           for (var i=0;i<researcher.genes.length;i++) {
//	              if ( researcher.genes[i].equals(specificGene) ) {
//	                 var specificTier = researcher.genes[i].tier;
//	              }
//
//	           }
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
	    
	}
	
	listResearchers.executeAjax = function(ajax_url, data) {
	   data = data || {};
      var promise = $.ajax( {
         cache : false,
         url : ajax_url,
         data : data,
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
	   utility.hideMessage($("#listResearchersMessage"));
	    $.ajax({
	        cache : false,
	        type : 'GET',
	        url : "loadAllResearchers.html",
	        success : function(response, xhr) {
	            response = $.parseJSON(response);
	            if ( response.success != true ) {
	               console.log("Failed to load all researchers", response);
	               return;
	            }
	            var researchers = [];
	            var i = 0;
	            $.each(response.data, function(index, r) {
	               var researcher = new researcherModel.Researcher();
	               researcher.parseResearcherObject( r )
	               researcher.index = i;
	               researchers.push( researcher );
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
	       utility.showMessage("Please select a gene", $("#listResearchersMessage"));
	        return;
	    } else {
	       utility.hideMessage( $("#listResearchersMessage") );
	    }
	    
	    gene = new researcherModel.Gene(gene);
	    
	    var researchers = [];
	    var tiers = [];
	    for (var i=0;i<allResearchers.length;i++) {
	       var specificGene = allResearchers[i].getGene(gene);
	       if ( specificGene !== null ) {
	          researchers.push(allResearchers[i]);
	          tiers.push( specificGene.tier );
	       }
	    }
	    
	    $("#listResearchersTable").DataTable().column(7).visible(true);
	    populateResearcherTable(researchers, tiers);

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

	            $('td:eq(0) > a', nRow).click(function (e) {

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
	              
	              // Closure to make sure that tab always returns the correct researcher
	              $('#registerTab li[id="tab'+nextTab+'"]').on('show.bs.tab',(function(r) {
	                 return function() {
	                 overview.showOverview( r, true, true, false);
	                 };
	                 })(allResearchers[index])
	              );           
	              
	              // make the new tab active
	              $('#registerTab a:last').tab('show');
	           });
	            
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
        	$("#updateCache").click(function() {return listResearchers.executeAjax("updateCache.html");})
        	$("#resetGeneTable").click(function() {return listResearchers.executeAjax("resetGeneTable.html");})
        	$("#resetGeneAnnotationTable").click(function() {return listResearchers.executeAjax("resetGeneAnnotationTable.html");})
        	
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