   /**
    * @memberOf researcherModel
    */
(function( researcherModel, $, undefined ) {
   
   var researcher = {}; //encapsulating in researcher object to make it easier to jsonify
   // Profile
   researcher.firstName = "";
   researcher.lastName = "";
   researcher.organization = "";
   researcher.department = "";
   researcher.website = "";
   researcher.phone = "";
   researcher.description = "";
   researcher.email = "";
   
   // Model Organisms
   researcher.genes = [];
   researcher.taxonDescriptions = {};
   
   // Profile
   // Setters
   researcherModel.setFirstName = function(val) {
	   researcher.firstName = val || "";
   }
   researcherModel.setLastName = function(val) {
	   researcher.lastName = val || "";
   }
   researcherModel.setOrganization = function(val) {
	   researcher.organization = val || "";
   }
   researcherModel.setDepartment = function(val) {
	   researcher.department = val || "";
   }
   researcherModel.setWebsite = function(val) {
      if ( val ) {
         // Prepend for absolute path
         val = val.indexOf("http://") === 0 ? val : "http://" + val;
         researcher.website = val;
      }
   }
   researcherModel.setPhone = function(val) {
	   researcher.phone = val || "";
   }
   researcherModel.setDescription = function(val) {
	   researcher.description = val || "";
   }
   setEmail = function(val) { // Private for now
	   researcher.email = val || "";
   }
   
   // Getters
   researcherModel.getFirstName = function() {
      return researcher.firstName;
   }
   researcherModel.getLastName = function() {
      return researcher.lastName;
   }
   researcherModel.getOrganization = function() {
      return researcher.organization;
   }
   researcherModel.getDepartment = function() {
      return researcher.department;
   }
   researcherModel.getWebsite = function() {
      return researcher.website;
   }
   researcherModel.getPhone = function() {
      return researcher.phone;
   }
   researcherModel.getDescription = function() {
      return researcher.description;
   }
   researcherModel.getEmail = function() {
	      return researcher.email;
	   }

   // Model Organisms Genes
   researcherModel.setGenes = function(genes) {
	   researcher.genes = genes.slice(); //set to clone of input, not reference
   }
   researcherModel.getGenes = function() {
      return researcher.genes.slice(); // return clone of genes array, not reference
   }
   researcherModel.addGene = function(gene) {
	   researcher.genes.push( gene );
   }
   researcherModel.addGenes = function(genes) {
       for (var i=0; i<genes.length; i++) {
    	   researcherModel.addGene( genes[i] );
       }
   }
   removeGene = function(gene) {
	   var index = indexOf(gene, researcher.genes);
	   if ( index > -1 ) {
		   researcher.genes.splice(index, 1);
	   }
   }
   removeGenes = function(genes) { // This could be faster...
       for (var i=0; i<genes.length; i++) {
    	   researcherModel.removeGene( genes[i] );
       }
   }
   isEqual = function(gene1, gene2) {
	   return ( gene1.officialSymbol === gene2.officialSymbol && gene1.taxon === gene2.taxon );
   }
   indexOf = function(gene, genes) {
       for (var i=0; i<genes.length; i++) {
    	   if ( isEqual(genes[i], gene) ) {
    		   return i;
    	   }
       }
       return -1;
   }
   
   // Taxon Descriptions
   researcherModel.setTaxonDescriptionsFromArray = function(taxonDescriptionsArr) {
      researcher.taxonDescriptions = {};
      for (var i=0;i<taxonDescriptionsArr.length;i++) {
         researcher.taxonDescriptions[taxonDescriptionsArr[i].taxon] = taxonDescriptionsArr[i].description;
      }
   }
   researcherModel.getTaxonDescriptions = function() {
      return researcher.taxonDescriptions;
   }
   researcherModel.addTaxonDescription = function(taxon, taxonDescription) {
      return researcher.taxonDescriptions[taxon] = taxonDescription;
   }
   taxonDescriptionsToJSON = function() {
      return $.toJSON( researcher.taxonDescriptions );
   }
   
   
   
   // High level functionality
   researcherModel.toJson = function() {
	   return $.toJSON(researcher);
   }
   researcherModel.genesToJSON = function(genes) {
	   var jsonArr = [];
	   for (var i=0; i<genes.length; i++) {
		   jsonArr.push( $.toJSON( genes[i] ) );
	   }
	   return jsonArr.slice();
   }
   
   researcherModel.aliasesToString = function(gene) {
	   arr = [];
	   for (var i=0; i<gene.aliases.length; i++) {
		   arr.push( gene.aliases[i].alias );
	   }
	   return arr.join( ', ' );
   
	}
   
   researcherModel.compareGenes = function(genes1, genes2) { // This is ugly...
      g1 = genes1.slice();
      g2 = genes2.slice();
      
      identicalIndexOf = function(gene,genes) { //Harsher equality check than indexOf
         for (var i=0; i<genes.length; i++) {
            if ( genes[i].officialSymbol === gene.officialSymbol && genes[i].taxon === gene.taxon && genes[i].tier === gene.tier) {
               return i;
            }
          }
          return -1;
      }
/*      if ( g1.length != g2.length ) { // Duplicates counted as different
         return false;
      }*/
      
      for (var i=0; i<genes1.length; i++) {
         var index = identicalIndexOf(genes1[i], genes2);
         if ( index == -1 ) {
            return false;
         }
      }
      
      for (var i=0; i<genes2.length; i++) {
         var index = identicalIndexOf(genes2[i], genes1);
         if ( index == -1 ) {
            return false;
         }
      }
      
      return true;
      
   }
   
   researcherModel.loadResearcher = function() {
	   
	   var promise = $.ajax( {
	      // cache : false,
	      // type : 'GET',
	      url : "loadResearcher.html",
	      success : function(response, xhr) {

	         var data = jQuery.parseJSON( response ).data;
	         
	         console.log("Loaded researcher:", data);
	         
	         researcherModel.setDepartment(data.department);
	         researcherModel.setOrganization(data.organization);
	         researcherModel.setWebsite(data.website);
	         researcherModel.setPhone(data.phone);
	         researcherModel.setDescription(data.description);
	         researcherModel.setTaxonDescriptionsFromArray(data.taxonDescriptions);
	         
	         var genes = [];
	         //This is ugly but for now with just two tiers and unknown it will do
	         if ("TIER1" in data.genes) {
   	         for (var i=0;i<data.genes.TIER1.length;i++) {
   	            data.genes.TIER1[i].tier = "TIER1";
   	         }
   	         genes = genes.concat(data.genes.TIER1);
	         }
	         if ("TIER2" in data.genes) {
               for (var i=0;i<data.genes.TIER2.length;i++) {
                  data.genes.TIER2[i].tier = "TIER2";
               }
               genes = genes.concat(data.genes.TIER2);
	         }
            if ("UNKNOWN" in data.genes) {
               for (var i=0;i<data.genes.UNKNOWN.length;i++) {
                  data.genes.UNKNOWN[i].tier = "UNKNOWN";
               }
               genes = genes.concat(data.genes.UNKNOWN);
            }
	         researcherModel.setGenes(genes);
	         
	         var contact;
	         
	         if ( data.contact != null ) {
	        	// Researcher created so Researcher object was returned
	            contact = data.contact;
	         } else {
	          // Researcher not created yet so User object was returned, this should not happen...
	            contact = data;
	         }
	         
	         researcherModel.setFirstName(contact.firstName);
	         researcherModel.setLastName(contact.lastName);
	         setEmail(contact.email);
	         //$("#overviewMessage").trigger("profileLoaded");
	         
	      },
	      error : function(response, xhr) {
	    	  console.log(response.message);
	      }
	   } );
	   
	   return promise;
	   
	}
/*   researcherModel.loadResearcherGenes = function() {
	   
	   var promise = $.ajax( {
	      url : "loadResearcherGenes.html",

	      data : {
	         taxonCommonName : "All",
	      },
	      dataType : "json",

	      success : function(response, xhr) {

	         if ( !response.success ) {
	        	 console.log(response.message);
	            return;
	         }

	         console.log( "Loaded researcher genes:", response.data);
	         
	         //researcherModel.setGenes(response.data);
	         //$("#overviewModelMessage").trigger("genesLoaded");
	         
	      },
	      error : function(response, xhr) {
	    	  console.log(response.message);
	      }
	   } );
	   
	   return promise;
	}*/
   
   researcherModel.saveResearcherProfile = function() {
      
	   var promise = $.ajax( {
	      cache : false,
	      type : 'POST',
	      url : 'saveResearcher.html',
	      data : researcher,
	      dataType : "json",
	      success : function(response, xhr) {
	         	         
	         if ( !response.success ) {
	            console.log( jQuery.parseJSON( response ).error );
	         }
	         
	      },
	      error : function(response, xhr) {
	         console.log( xhr.responseText );
	      }
	   } );
	   
	   return promise;
	  
   }
   
   researcherModel.saveResearcherGenes = function() {
	   console.log("saving genes: ", researcherModel.getGenes())
	   	   
	   var promise = $.ajax( {
	      type: "POST",
	      url : "saveResearcherGenes.html",

	      data : {
	         genes : researcherModel.genesToJSON( researcherModel.getGenes() ),
	         taxonDescriptions: taxonDescriptionsToJSON()
	      },
	      dataType : "json"
	   } );
	   
	   return promise;
	}
   
   
   
}( window.researcherModel = window.researcherModel || {}, jQuery ));
