   /**
    * @memberOf admin
    */
(function( admin, $, undefined ) {
   
   admin.table = function() {
      return $('#researchersTable');
   }
   
   admin.select2 = function() {
      return $('#adminSearchGenesSelect');
   }
   
   admin.taxonSelect = function() {
      return $('#adminTaxonCommonNameSelect');
   }
   
   admin.tierSelect = function() {
      return $('#adminTierSelect');
   }
   
   var oTable;
   
   var allResearchers;
   
   populateResearcherTable = function(researchers, tiers) {
      table = admin.table().DataTable();
       table.clear().draw();
       $.each(researchers, function(index, researcher) {
           
           if ( tiers ) {
              var specificTier = tiers[index];
//            for (var i=0;i<researcher.genes.length;i++) {
//               if ( researcher.genes[i].equals(specificGene) ) {
//                  var specificTier = researcher.genes[i].tier;
//               }
//
//            }
           }
            
           var termsLength = 0;
           var taxonIds = Object.keys(researcher.terms);
           for ( var i=0;i<taxonIds.length;i++) {
              termsLength+= researcher.terms[taxonIds[i]].length;
           }
   
           var researcherRow = [ researcher.index,
                                 researcher.email || "",
                                 researcher.firstName || "", 
                                 researcher.lastName || "",
                                 researcher.organization || "", 
                                 researcher.genes.length || 0,
                                 termsLength || 0,
                                 specificTier || ""]
           table.row.add(researcherRow).draw();
       });
       
   }
   
   resetTable = function() {
      var dTable = admin.table().DataTable();
      dTable.column(dTable.columns()[0].length - 1).visible(false);
      populateResearcherTable(allResearchers);
   }
   
   admin.getResearchers = function() {
      //utility.hideMessage($("#listResearchersMessage"));
       var promise = $.ajax({
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
               //$('#registerTab a[href="#registeredResearchers"]').show();
               var dTable = admin.table().DataTable();
               dTable.column(dTable.columns()[0].length - 1).visible(false);
               //admin.table().DataTable().column(7).visible(false);
               //$( "#findResearchersByGenesSelect" ).select2("val", "");
               populateResearcherTable(researchers);
               
           },
           error : function(response, xhr) {
               console.log(xhr.responseText);
/*               $("#listResearchersMessage").html(
                       "Error with request. Status is: " + xhr.status + ". "
                               + jQuery.parseJSON(response).message);*/
               //$("#listResearchersFailed").show();
           }
       });
       return promise;
   }
   
   admin.findResearchersByGene = function() {

       var gene = admin.select2().select2("data")
   
       if (gene == null) {
          console.log("Please select a gene")
          //utility.showMessage("Please select a gene", $("#listResearchersMessage"));
           return;
       } else {
          //utility.hideMessage( $("#listResearchersMessage") );
       }
       
       gene = new researcherModel.Gene(gene);
       var tierSelect = admin.tierSelect().val();
       var researchers = [];
       var tiers = [];
       for (var i=0;i<allResearchers.length;i++) {
          var specificGene = allResearchers[i].getGene(gene);
          if ( specificGene !== null ) {
             if ( !tierSelect || specificGene.tier == tierSelect ) {
             researchers.push(allResearchers[i]);
             tiers.push( specificGene.tier );
             }
          }
       }
       var dTable = admin.table().DataTable();
       dTable.column(dTable.columns()[0].length - 1).visible(true);
       populateResearcherTable(researchers, tiers);

   }
   
   removeTab = function(tabId,  navbar) {
      navbar.find('a[href="'+tabId+'"]').closest('li').remove();
      $(tabId).remove();
   }
   
   admin.initDataTable = function() {
      // Initialize datatable
      oTable = admin.table().dataTable( {
         "scrollY":        "400px",
         "scrollCollapse": true,
         "paging":         false,
         "order": [ 1, "asc" ],
         "aoColumnDefs": [ 
           {
              "aTargets": [ 0 ],
              "visible":false,
              "defaultContent": "",
              "searchable":false
           }],
         "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
            // Keep in mind that $('td:eq(0)', nRow) refers to the first DISPLAYED column
            // whereas aData[0] refers to the data in the first column, hidden or not
            $('td:eq(0)', nRow).html('<a href="#view'+aData[0]+'"><i class="fa fa-search fa-fw yellow-icon"></i></a>&nbsp;'+aData[1]);
            //$('td:eq(0)', nRow).html('<a href="#displayResearcher' + aData[0] + '">'+ aData[1] + '</a>');

            $('td:eq(0) > a', nRow).click(function (e) {

               var index = $(this).attr('href').substr(5); // Length of '#displayResearcher'
               if ( index ==="" ) {
                  // Something went wrong when trying to attach the researchers index to the hyperlink
                  console.log("Cannot find researcher!")
                  return;
               }
               index = parseInt(index);
               
               if ( $('#researcherTab-'+index).length > 0 ) {
                  return;
               }
               
               tabName = allResearchers[index].userName;
               
              var nextTab = $('#admin-nav-tabs li').size()+1;
              
              // create the tab
              $('<li id="researcherView'+nextTab+'"><a href="#researcherTab-'+index+'" data-toggle="tab"><button class="close" title="Remove this page" type="button">&nbsp; <i class="fa fa-close red-icon"></i> </button><i class="fa fa-user"></i> '+tabName+'</a></li>').appendTo('#admin-nav-tabs');
              $('#admin-nav-tabs a[href="#researcherTab-'+index+'"] button').click( function() {
                 var tabId = '#researcherTab-'+index;
                 var navbar = $('#admin-nav-tabs');
                 $('#admin-nav-tabs a:first').click();
                 removeTab(tabId, navbar);
                 
              })
              var clone = $('#profile-tab').children('.row').clone()
              clone.find('i').closest('a').replaceWith('<i class="fa fa-info-circle fa-fw"></i>');
              $('#admin .tab-content').append('<div class="tab-pane profile" id="researcherTab-'+index+'"></div>')
              $('#researcherTab-'+index).append( clone );
              profile.setInfo( allResearchers[index], '#researcherTab-'+index );
              
              var tables = $( 
                 '<div class="row">\
                    <div class="col-sm-6 scrollable-y">\
                       <table class="table table-bordered table-condensed stripe text-left display gene-table" cellspacing="0" width="100%">\
                          <thead>\
                             <tr>\
                                <th>Symbol</th>\
                                <th>Organism</th>\
                                <th>Tier</th>\
                             </tr>\
                          </thead>\
                          <tbody>\
                          </tbody>\
                       </table>\
                    </div>\
                    <div class="col-sm-6 scrollable-y">\
                       <table class="table table-bordered table-condensed stripe text-left display go-table" cellspacing="0" width="100%">\
                          <thead>\
                             <tr>\
                                <th>GO ID</th>\
                                <th>Aspect</th>\
                                <th>Organism</th>\
                             </tr>\
                          </thead>\
                          <tbody>\
                          </tbody>\
                       </table>\
                    </div>\
                  </div>' 
                 );
              var genes = allResearchers[index].genes;
              genes.sort(function(a, b){
                 if (a.taxon < b.taxon)
                    return -1;
                 if (a.taxon > b.taxon)
                   return 1;
                 return 0;
                 });
              for (var i=0;i<genes.length;i++) {
                 tables.find('table.gene-table tbody').append('<tr><td>'+genes[i].officialSymbol+'</td><td>'+genes[i].taxon+'</td><td>'+genes[i].tier+'</td></tr>')
              }
              
              var taxonIds = Object.keys(allResearchers[index].terms);
              for ( var i=0;i<taxonIds.length;i++) {
                 var terms = allResearchers[index].terms[taxonIds[i]];
                 for (var j=0;j<terms.length;j++) {
                    tables.find('table.go-table tbody').append('<tr><td>'+terms[j].geneOntologyId+'</td><td>'+terms[j].aspect+'</td><td>'+utility.taxonIdToName[taxonIds[i]]+'</td></tr>')
                 }
              }
              
              $('#researcherTab-'+index).append(tables);
              
              // make the new tab active
              $('#admin-nav-tabs a:last').click();
           });
            
            return nRow;
         },
         
      } );
   }
   
   admin.initSelect2 = function() {
      // init search genes combo    
      admin.select2().select2( {
         id : function(data) {
            return data.officialSymbol;
         },
         placeholder : "Select a gene symbol or name",
         minimumInputLength : 3,
         ajax : {
            url : "searchGenes.html",
            dataType : "json",
            data : function(query, page) {
               return {
                  query : query, // search term
                  taxonId : admin.taxonSelect().val()
               }
            },
            results : function(data, page) {
               // convert object to text symbol + text
               // select2 format result looks for the 'text' attr
               var geneResults = []
               for (var i = 0; i < data.data.length; i++) {
                  var gene = new researcherModel.Gene( data.data[i] );
                  var aliasStr = gene.aliasesToString();
                  aliasStr = aliasStr ? "(" + aliasStr + ") " : "";
                  gene.text = "<b>" + gene.officialSymbol + "</b> " + aliasStr + gene.officialName
                  geneResults.push(gene);
               }
               return {
                  results : geneResults
               };
            },

         },
         formatSelection : function(item) {
            item.text = item.text.split('</b>')[0] + '</b>';
            return item.text
         },
         formatAjaxError : function(response) {
            var msg = response.responseText;
            return msg;
         }, 
         // we do not want to escape markup since we are displaying html in results
         escapeMarkup : function(m) {
            return m;
         },
      } );
   }
   
      admin.init = function(){
         admin.initDataTable();
         admin.initSelect2();
         var promise = admin.getResearchers();
         $('#menu > li a[href="#admin"][data-toggle="tab"]').on('shown.bs.tab',function() {
            oTable.fnAdjustColumnSizing();
         });
         $('#admin-nav-tabs > li a[href="#researchers-tab"][data-toggle="tab"]').on('shown.bs.tab',function() {
            oTable.fnAdjustColumnSizing();
         });
         $("#adminResetResearchersButton").click(resetTable)
         $("#adminFindResearchersByGeneButton").click(admin.findResearchersByGene)         
      };
}( window.admin = window.admin || {}, jQuery ));

// Initialize document
$( document ).ready( function() {
   
} );