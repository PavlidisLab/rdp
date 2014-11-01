   /**
    * @memberOf researcherModel
    */
(function( researcherModel, $, undefined ) {
   
   researcherModel.Researcher = function Researcher() {
      this.userName = "";
      this.firstName = "";
      this.lastName = "";
      this.organization = "";
      this.department = "";
      //this.website = "";
      this.phone = "";
      this.description = "";
      this.email = "";
      
      var _website = "";
      Object.defineProperty(this, "website", { 
          configurable: false, // Immutable property
          enumerable:true,
          get: function() { return _website; }, 
          set: function(val) { 
             if ( val ) {
                // Prepend for absolute path
                val = val.indexOf("http://") === 0 ? val : "http://" + val;
                _website = val;
             } 
          } 
      });
      
      this.pubMedIds = [];
      
      // Model Organisms
      this.genes = [];
      this.calculatedGenes = [];
      this.terms = {};
      this.taxonDescriptions = {};
   }
   
   researcherModel.Researcher.prototype.indexOfGene = function(gene) {
      if ( !(gene instanceof researcherModel.Gene) ) {
         console.log( "Not a Gene Object", gene )
      }
      for (var i=0;i<this.genes.length;i++) {
         if ( gene.equals(this.genes[i]) ) {
            return i;
         }
      }
      return -1;
      
   }
   
   // Used to determine if a researcher has a gene in either saved or calculated genes
   researcherModel.Researcher.prototype.getGene = function(gene) {
      if ( !(gene instanceof researcherModel.Gene) ) {
         console.log( "Not a Gene Object", gene );
         return null;
      }
      for (var i=0;i<this.genes.length;i++) {
         if ( gene.equals(this.genes[i]) ) {
            return this.genes[i];
         }
      }
      for (var i=0;i<this.calculatedGenes.length;i++) {
         if ( gene.equals(this.calculatedGenes[i]) ) {
            return this.calculatedGenes[i];
         }
      }
      return null;

   }
   
   researcherModel.Researcher.prototype.getTaxons = function() {
      var taxons = [];
      //var taxonIds = Object.keys(utility.taxonIdToName);
      var genes = this.genes;
      for (var i=0;i<genes.length;i++) {
         var taxonId = genes[i].taxonId;
         if ( taxons.indexOf(taxonId) == -1 ) {
            taxons.push(taxonId);
         }
      }
      return taxons;
   }
   
   researcherModel.Researcher.prototype.shallowCopy = function() {
      // For some reason jquery AJAX doesn't like sending instances of Researcher
      // straight in the data
      return $.parseJSON($.toJSON(this));
   }
   
   researcherModel.Researcher.prototype.fullName = function() { 
      return this.firstName && this.lastName ? this.firstName + " " + this.lastName : this.firstName + this.lastName;
   }
   
   researcherModel.Researcher.prototype.setTaxonDescriptionsFromArray = function(taxonDescriptionsArr) {
      this.taxonDescriptions = {};
      for (var i=0;i<taxonDescriptionsArr.length;i++) {
         this.taxonDescriptions[taxonDescriptionsArr[i].taxonId] = taxonDescriptionsArr[i].description;
      }
   }
   
   researcherModel.Researcher.prototype.setPubMedIdsFromPublications = function(publicationsArray) {
      this.pubMedIds = [];
      for (var i=0;i<publicationsArray.length;i++) {
         this.pubMedIds.push( publicationsArray[i].pubMedId );
      }
   }
   
   researcherModel.Researcher.prototype.setTermsFromArray = function(goTermsArr) {
      this.terms = {};
      if ( goTermsArr ) {
      for (var i=0;i<goTermsArr.length;i++) {
         if ( this.terms.hasOwnProperty( goTermsArr[i].taxonId ) ) {
            this.terms[goTermsArr[i].taxonId].push( goTermsArr[i] );
         } else {
            this.terms[goTermsArr[i].taxonId] = [ goTermsArr[i] ];
         }
      }
      }
   }
   
   researcherModel.Researcher.prototype.updateTermsForTaxon = function(goTermsArr, taxonId) {
      delete this.terms[taxonId];
      this.terms[taxonId] = [];
      for (var i=0;i<goTermsArr.length;i++) {
         this.terms[taxonId].push( goTermsArr[i] );
      }
   }
      
   researcherModel.Researcher.prototype.addTaxonDescription = function(taxon, taxonDescription) {
      this.taxonDescriptions[taxon] = taxonDescription;
   }
   
   researcherModel.Researcher.prototype.genesToJSON = function() {
      var jsonArr = [];
      for (var i=0; i<this.genes.length; i++) {
         jsonArr.push( $.toJSON( this.genes[i] ) );
      }
      return jsonArr;
   }
   
   researcherModel.Researcher.prototype.termsToJSON = function(taxonId) {
      if ( !taxonId ) {
         return null;
      }
      
      var jsonArr = [];
      for (var i=0; i<this.terms[taxonId].length; i++) {
         jsonArr.push( $.toJSON( this.terms[taxonId][i] ) );
      }
      return jsonArr;
   }
    
   researcherModel.Researcher.prototype.compareGenes = function(otherGenes) {
      g1 = otherGenes.slice();
      g2 = this.genes.slice();
      
      identicalIndexOf = function(gene,genes) { //Harsher equality check than indexOf
         for (var i=0; i<genes.length; i++) {
            if ( genes[i].id === gene.id && genes[i].tier === gene.tier) {
               return i;
            }
          }
          return -1;
      }
      
      for (var i=0; i<g1.length; i++) {
         var index = identicalIndexOf(g1[i], g2);
         if ( index == -1 ) {
            return false;
         }
      }
      
      for (var i=0; i<g2.length; i++) {
         var index = identicalIndexOf(g2[i], g1);
         if ( index == -1 ) {
            return false;
         }
      }
      
      return true;
   }
   
   researcherModel.Researcher.prototype.parseResearcherObject = function(data) {
      this.department = data.department || "";
      this.organization = data.organization || "";
      this.website = data.website;
      this.phone = data.phone || "";
      this.description = data.description || "";
      this.setPubMedIdsFromPublications( data.publications );
      this.setTaxonDescriptionsFromArray(data.taxonDescriptions);
      this.setTermsFromArray(data.terms);

      var genes = [];
      var calculateGenes = [];
      for (var i=0;i<data.genes.length;i++) {
         var g = new researcherModel.Gene( data.genes[i] );
         if ( data.genes[i].tier === "TIER3" ) {
            calculateGenes.push( g );                     
         } else {
            genes.push( g );
         }
      }
      
      this.genes = genes;
      this.calculatedGenes = calculateGenes;
      
      var contact;
      
      if ( data.contact != null ) {
      // Researcher created so Researcher object was returned
         contact = data.contact;
      } else {
       // Researcher not created yet so User object was returned, this should not happen...
         contact = data;
      }
      
      this.firstName = contact.firstName || "";
      this.lastName = contact.lastName || "";
      this.email = contact.email || "";
      this.userName = contact.userName || "";
   }
   
   researcherModel.Gene = function Gene(geneValueObject) {
      this.id = geneValueObject.id || null;
      this.ncbiGeneId = geneValueObject.id || null;
      this.officialName = geneValueObject.officialName || null;
      this.officialSymbol = geneValueObject.officialSymbol || null;
      this.taxon = geneValueObject.taxonCommonName || null;
      this.taxonId = geneValueObject.taxonId || null;
      this.tier = geneValueObject.tier || "TIER2";

      if ( !geneValueObject.aliases ) {
         this.aliases = [];
      } else {
         if (geneValueObject.aliases instanceof Array) {
            this.aliases = geneValueObject.aliases;
         } else {
            this.aliases = geneValueObject.aliases.split("|");
         }
      }
   }
   
   researcherModel.Gene.prototype.equals = function(otherGene) {
      if ( !(otherGene instanceof researcherModel.Gene) ) {
         return false;
      }
      if (otherGene.id === this.id) {
         return true;
      }
      else {
         return false;
      }
      
   }
   
   researcherModel.Gene.prototype.aliasesToString = function() {
      return this.aliases.join( ', ' );
   }
   
   researcherModel.Gene.prototype.clone = function() {
      return new researcherModel.Gene(this);
   }
   
   researcherModel.currentResearcher = new researcherModel.Researcher();
   
   researcherModel.loadResearcher = function() {
	   
	   var promise = $.ajax( {
	      // cache : false,
	      // type : 'GET',
	      url : "loadResearcher.html",
	      success : function(response, xhr) {
	         
	         var  json = jQuery.parseJSON( response );
	         
	         if (json.success==true) {
	            
   	         var data = jQuery.parseJSON( response ).data;
   	         
   	         //console.log("Loaded researcher:", data);
   	         
   	         researcherModel.currentResearcher.parseResearcherObject(data);
   	         
   	         console.log("Loaded researcher:", researcherModel.currentResearcher);
	         
	         } else {
	            console.log("Failed to load researcher:", json.message);
	         }

	      },
	      error : function(response, xhr) {
	    	  console.log(response.message);
	      }
	   } );
	   
	   return promise;
	   
	}
   
   researcherModel.saveResearcherProfile = function() {
	   var promise = $.ajax( {
	      cache : false,
	      type : 'POST',
	      url : 'saveResearcher.html',
	      data : researcherModel.currentResearcher.shallowCopy(),
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
	   console.log("saving genes: ", researcherModel.currentResearcher.genes)
	   	   
	   var promise = $.ajax( {
	      type: "POST",
	      url : "saveResearcherGenes.html",

	      data : {
	         genes : researcherModel.currentResearcher.genesToJSON(),
	         taxonDescriptions: $.toJSON( researcherModel.currentResearcher.taxonDescriptions )
	      },
	      dataType : "json"
	   } );
	   
	   return promise;
	}  
   
   researcherModel.saveResearcherTermsForTaxon = function(taxId) {
      if ( !taxId ){
         return null;
      }
      console.log("saving terms: ", researcherModel.currentResearcher.terms[taxId])
            
      var promise = $.ajax( {
         type: "POST",
         url : "saveResearcherGOTerms.html",

         data : {
            terms : researcherModel.currentResearcher.termsToJSON(taxId),
            taxonId: taxId
         },
         dataType : "json"
      } );
      
      return promise;
   } 
   
}( window.researcherModel = window.researcherModel || {}, jQuery ));
