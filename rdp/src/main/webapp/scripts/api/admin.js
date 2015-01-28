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
   
   admin.advancedSymbol = function() {
      return $('#admin-advanced input.symbol-pattern');
   }
   
   admin.advancedTaxon = function(override) {
      override = utility.isUndefined(override) ? "#admin-advanced" : override;
      return $(override + ' select.taxon');
   }
   
   admin.advancedTier = function() {
      return $('#admin-advanced select.tier');
   }
   
   admin.advancedId = function() {
      return $('#admin-advanced-term input.id-pattern');
   }
   
   admin.advancedTerm = function() {
      return $('#admin-advanced-term input.term-pattern');
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
           table.row.add(researcherRow);
       });
       table.draw();
       oTable.fnAdjustColumnSizing();
       
   }
   
   admin.resetTable = function() {
      // TODO The updating of the spinner doesn't work
      $('#menu > li a[href="#admin"][data-toggle="tab"] span.fa-spin').addClass("fa-spin");
      var dTable = admin.table().DataTable();
      dTable.column(dTable.columns()[0].length - 1).visible(false);
      populateResearcherTable(allResearchers);
      $('#menu > li a[href="#admin"][data-toggle="tab"] span.fa-spin').removeClass("fa-spin");
      utility.hideMessage($("#researchers-tab .alert div"));
   }
   
   var testFunction = function() {
      
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
               console.log(xhr);
               console.log(response);
/*               $("#listResearchersMessage").html(
                       "Error with request. Status is: " + xhr.status + ". "
                               + jQuery.parseJSON(response).message);*/
               //$("#listResearchersFailed").show();
           }
       });
       return promise;
   }
   
   admin.findResearchersByGene = function() {
      utility.hideMessage( $("#researchers-tab .alert div") );
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
       utility.showMessage("Found " + researchers.length + " matching researchers", $("#researchers-tab .alert div"));
       populateResearcherTable(researchers, tiers);

   }
   
   admin.advancedSymbolSearch = function() {
      utility.hideMessage( $("#researchers-tab .alert div") );
       var symbolPattern = admin.advancedSymbol().val();
   
       if (symbolPattern == null || symbolPattern == "") {
          //console.log("Please select a symbol pattern")
          utility.showMessage("Please select a symbol pattern", $("#researchers-tab .alert div"));
           return;
       } else {
          //utility.hideMessage( $("#listResearchersMessage") );
       }
       
       var tierSelect = admin.advancedTier().val();
       var tId = parseInt(admin.advancedTaxon().val());
       var researchers = [];
       var tiers = [];
       console.log(symbolPattern);
       console.log(tierSelect);
       console.log(tId);
       for (var i=0;i<allResearchers.length;i++) {
          var foundGenes = allResearchers[i].getGenesBySymbolPattern(symbolPattern, tId, "i");
          if ( foundGenes.status ) {
             foundGenes = foundGenes.genes;
             if ( foundGenes !== null && foundGenes.length > 0) {
                var bestTier = null;
                for (var j=0;j<foundGenes.length;j++) {
                   var g = foundGenes[j];
                   if ( !tierSelect || g.tier == tierSelect) {
                      if ( bestTier == null || g.tier < bestTier ) {
                         bestTier = g.tier;
                      }
                   }
                }
                
                if ( bestTier !== null ) {
                   console.log(allResearchers[i]);
                   console.log(foundGenes)
                   researchers.push(allResearchers[i]);
                   tiers.push( bestTier );
                }
             }
          } else {
             utility.showMessage(foundGenes.error, $("#researchers-tab .alert div"));
             return;
          }
       }
       var dTable = admin.table().DataTable();
       dTable.column(dTable.columns()[0].length - 1).visible(true);
       utility.showMessage("Found " + researchers.length + " matching researchers", $("#researchers-tab .alert div"));
       populateResearcherTable(researchers, tiers);

   }
   
   admin.advancedTermSearch = function() {
      utility.hideMessage( $("#researchers-tab .alert div") );
       var TermPattern = admin.advancedTerm().val();
       var IdPattern = admin.advancedId().val();
       console.log(TermPattern, IdPattern)
 
       if ( (TermPattern == null || TermPattern == "") && (IdPattern == null || IdPattern == "") ) {
          //console.log("Please select a symbol pattern")
          utility.showMessage("Please select a term pattern and/or id pattern", $("#researchers-tab .alert div"));
           return;
       } else {
          //utility.hideMessage( $("#listResearchersMessage") );
       }
       
       var tId = parseInt(admin.advancedTaxon("#admin-advanced-term").val());
       var researchers = [];
       for (var i=0;i<allResearchers.length;i++) {
          var terms = allResearchers[i].getTermsByPattern(TermPattern, IdPattern, tId, "i");
          
          if ( terms.status ) {
             terms = terms.terms;
             if ( terms !== null && terms.length > 0) {
                researchers.push(allResearchers[i]);
             }
          } else {
             utility.showMessage(foundGenes.error, $("#researchers-tab .alert div"));
             return;
          }
       }
       var dTable = admin.table().DataTable();
       dTable.column(dTable.columns()[0].length - 1).visible(false);
       utility.showMessage("Found " + researchers.length + " matching researchers", $("#researchers-tab .alert div"));
       populateResearcherTable(researchers);

   }
   
   admin.removeTab = function(tabId,  navbar) {
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
           dom: 'T<"clear">lfrtip',
           tableTools: {
              "aButtons": [ {"sExtends":    "text", "fnClick":function() { return admin.resetTable()}, "sButtonText": '<a href="#"><i class="fa fa-refresh"></i></a>' }]
           },
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
                 admin.removeTab(tabId, navbar);
                 
              })
              var clone = $('#profile-tab').children('.row').clone()
              clone.find('i').closest('a').replaceWith('<i class="fa fa-info-circle fa-fw"></i>');
              $('#admin .tab-content').append('<div class="tab-pane profile" id="researcherTab-'+index+'"></div>')
              $('#researcherTab-'+index).append( clone );
              profile.setInfo( allResearchers[index], '#researcherTab-'+index );
                            
              var accordion = $(
                 '<div class="panel-group" id="admin-researcher-accordion-'+ index +'" role="tablist"\
                    aria-multiselectable="true">\
                 </div>'
                 
                 );
              
              var getPanel = function(id, title) {
                return $('<div class="panel panel-default">\
                   <div data-toggle="collapse" data-target="#admin-researcher-collapse-'+ index +'-'+id+'"\
                   data-parent="#admin-researcher-accordion-'+ index +'" aria-expanded="false"\
                   aria-controls="admin-researcher-collapse-'+ index +'-'+id+'" class="panel-heading" role="tab"\
                   id="admin-researcher-heading-'+ index +'-'+id+'">\
                   <h4 class="panel-title">\
                      <a href="#"> '+title+': </a>\
                   </h4>\
                </div>\
                <div id="admin-researcher-collapse-'+ index +'-'+id+'" class="panel-collapse collapse"\
                   role="tabpanel" aria-labelledby="admin-researcher-heading-'+ index +'-'+id+'">\
                   <div class="panel-body">\
                   </div>\
                </div>\
             </div>'              
             ); 
              }
             
              var tables = $( 
                 '<div class="row">\
                    <div class="col-sm-6">\
                       <table class="table table-bordered table-condensed stripe text-left display gene-table" cellspacing="0" width="100%">\
                          <thead>\
                             <tr>\
                                <th>Symbol</th>\
                                <th>Tier</th>\
                                <th>Name</th>\
                             </tr>\
                          </thead>\
                          <tbody>\
                          </tbody>\
                       </table>\
                    </div>\
                    <div class="col-sm-6">\
                       <table class="table table-bordered table-condensed stripe text-left display go-table" cellspacing="0" width="100%">\
                          <thead>\
                             <tr>\
                                <th>GO ID</th>\
                                <th>Aspect</th>\
                                <th>Name</th>\
                             </tr>\
                          </thead>\
                          <tbody>\
                          </tbody>\
                       </table>\
                    </div>\
                  </div>' 
                 );
              
              var r = allResearchers[index];
              var sortedTaxons = r.getTaxons().concat(Object.keys(allResearchers[index].terms));
              sortedTaxons = utility.uniqueSoft(sortedTaxons).sort(function(a, b){return a-b});
              
              for (var i=0;i<sortedTaxons.length;i++) {
                 var id = sortedTaxons[i];
                 var g = r.getGenesByTaxonId(id);
                 var cg = r.getCalculatedGenesByTaxonId(id);
                 var t = r.terms[id];
                 
                 var newTables = tables.clone();
                 
                 if ( g ) {
                    for (var j=0;j<g.length;j++) {
                       newTables.find('table.gene-table tbody').append('<tr><td>'+g[j].officialSymbol+'</td><td>'+g[j].tier+'</td><td>'+g[j].officialName+'</td></tr>')
                    }
                 }
                 
                 if ( cg ) {
                    for (var j=0;j<cg.length;j++) {
                       newTables.find('table.gene-table tbody').append('<tr><td>'+cg[j].officialSymbol+'</td><td>'+cg[j].tier+'</td><td>'+cg[j].officialName+'</td></tr>')
                    }
                 }
                 
                 if ( t ) {
                    for (var j=0;j<t.length;j++) {
                       newTables.find('table.go-table tbody').append('<tr><td>'+t[j].geneOntologyId+'</td><td>'+t[j].aspect+'</td><td>'+t[j].geneOntologyTerm+'</td></tr>')
                    }
                 }
                 
                 var panel = getPanel(id, utility.taxonIdToName[id]);
                 var des = r.taxonDescriptions[id] ? r.taxonDescriptions[id] : "";
                 panel.find('div.panel-body').append('<p class="data-paragraph"><b>Focus: </b>'+  des +'</p>')
                 panel.find('div.panel-body').append(newTables);
                 accordion.append(panel);                
                 
              }
                         
              $('#researcherTab-'+index).append(accordion);
              
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
         $.when(promise).done(function() {
            $('#menu > li a[href="#admin"][data-toggle="tab"] span.fa-spin').removeClass("fa-spin");
         });
         $('#menu > li a[href="#admin"][data-toggle="tab"]').on('shown.bs.tab',function() {
            oTable.fnAdjustColumnSizing();
         });
         $('#admin-nav-tabs > li a[href="#researchers-tab"][data-toggle="tab"]').on('shown.bs.tab',function() {
            oTable.fnAdjustColumnSizing();
         });
         //$("#adminResetResearchersButton").click(admin.resetTable)
         $("#adminFindResearchersByGeneButton").click(admin.findResearchersByGene)   
         $("#admin-advanced button.submit").click(admin.advancedSymbolSearch)
         $("#admin-advanced-term button.submit").click(admin.advancedTermSearch)
      };
}( window.admin = window.admin || {}, jQuery ));

// Initialize document
$( document ).ready( function() {
   
} );