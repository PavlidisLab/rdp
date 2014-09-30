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
      this.test = "";
      
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
      
      // Model Organisms
      this.genes = [];
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
         this.taxonDescriptions[taxonDescriptionsArr[i].taxon] = taxonDescriptionsArr[i].description;
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
    
   researcherModel.Researcher.prototype.compareGenes = function(otherGenes) {
      g1 = otherGenes.slice();
      g2 = this.genes.slice();
      
      identicalIndexOf = function(gene,genes) { //Harsher equality check than indexOf
         for (var i=0; i<genes.length; i++) {
            if ( genes[i].officialSymbol === gene.officialSymbol && genes[i].taxon === gene.taxon && genes[i].tier === gene.tier) {
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
      this.setTaxonDescriptionsFromArray(data.taxonDescriptions);
      
      var genes = [];

      for (var tier in data.genes) {
         if (data.genes.hasOwnProperty(tier)) {
            for (var i=0;i<data.genes[tier].length;i++) {
               data.genes[tier][i].tier = tier;
               genes.push( new researcherModel.Gene( data.genes[tier][i] ) );
            }
         }
      }
      
      this.genes = genes;
      
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
      this.ncbiGeneId = geneValueObject.ncbiGeneId || null;
      this.officialName = geneValueObject.officialName || null;
      this.officialSymbol = geneValueObject.officialSymbol || null;
      this.taxon = geneValueObject.taxon || null;
      this.tier = geneValueObject.tier || null;
      this.aliases = geneValueObject.aliases || [];
   }
   
   researcherModel.Gene.prototype.equals = function(otherGene) {
      if ( !(otherGene instanceof researcherModel.Gene) ) {
         return false;
      }
      if (otherGene.officialSymbol === this.officialSymbol && otherGene.taxon === this.taxon) {
         return true;
      }
      else {
         return false;
      }
      
   }
   
   researcherModel.Gene.prototype.aliasesToString = function() {
      arr = [];
      for (var i=0; i<this.aliases.length; i++) {
         arr.push( this.aliases[i].alias );
      }
      return arr.join( ', ' );
      
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

	         var data = jQuery.parseJSON( response ).data;
	         
	         //console.log("Loaded researcher:", data);
	         
	         researcherModel.currentResearcher.parseResearcherObject(data);
	         
	         console.log("Loaded researcher:", researcherModel.currentResearcher);

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
   
}( window.researcherModel = window.researcherModel || {}, jQuery ));
