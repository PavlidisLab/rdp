//namespace: researcherModel
(function( researcherModel, $, undefined ) {
   
   // Profile
   var firstName = "";
   var lastName = "";
   var organization = "";
   var department = "";
   var website = "";
   var phone = "";
   var researchInterests = "";
   
   // Model Organisms
   var genes = []; // gene object needs a key = "key" for comparison
   
   // Profile
   researcherModel.setFirstName = function(val) {
      firstName = val;
   }
   researcherModel.setLastName = function(val) {
      lastName = val;
   }
   researcherModel.setOrganization = function(val) {
      organization = val;
   }
   researcherModel.setDepartment = function(val) {
      department = val;
   }
   researcherModel.setWebsite = function(val) {
      website = val;
   }
   researcherModel.setPhone = function(val) {
      phone = val;
   }
   researcherModel.setResearchInterests = function(val) {
      researchInterests = val;
   }
   
   researcherModel.getFirstName = function(val) {
      return firstName;
   }
   researcherModel.getLastName = function(val) {
      return lastName;
   }
   researcherModel.getOrganization = function(val) {
      return organization;
   }
   researcherModel.getDepartment = function(val) {
      return department;
   }
   researcherModel.getWebsite = function(val) {
      return website;
   }
   researcherModel.getPhone = function(val) {
      return phone;
   }
   researcherModel.getResearchInterests = function(val) {
      return researchInterests;
   }

   // Model Organisms
   researcherModel.setGenes = function(genesArr) {
      genes = genesArr;
   }
   researcherModel.getGenes = function() {
      return genes;
   }
   researcherModel.addGene = function(gene) {
      genes.push(gene);
   }
   researcherModel.removeGene = function(gene) {
      genes.push(gene);
   }
   researcherModel.compareGenes = function(gene1,gene2) {
      return gene1.key === gene2.key;
   }
   researchModel.indexOf = function(gene) {
      for (var i = 0; i < genes.length; i++) {
         if ( researcherModel.compareGenes(genes[i],gene) ) {
             return i;
         }
     }
     return -1;
   }
   
   
   
}( window.researcherModel = window.researcherModel || {}, jQuery ));
