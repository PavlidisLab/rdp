function collectModel() {
    var model = {};

    // Research Information
    model.description = $.trim($('.research-info').find('.data-edit')[0].value);

    // Gene Information
    var geneTierMap = {};
    $('#genes').find('.gene-table').find('tbody > tr').each(function(idx) {
        var tds = $(this).find('td');

        // If id column contains an integer
        var geneId =  parseInt(tds[1].innerText, 10);
        if (tds[1].innerText == geneId ) {
            geneTierMap[geneId] = $(tds[3]).find('input[type=checkbox]').prop('checked') ? "TIER1" : "TIER2";
        }
    });
    model.geneTierMap = geneTierMap;

    // Term Information
    var goIds = [];
    $('#terms').find('.term-table').find('tbody > tr').each(function(idx) {
        goIds.push($.trim($(this).find('td')[0].innerText));
    });
    model.goIds = goIds;

    return model;
}

function addGeneRow(table, gene, backupSymbol) {
    var newRow = $("<tr class='new-row'></tr>");
    var cols = "";
    if ( gene === null ) {
        console.log("Issue obtaining metadata for: " + backupSymbol);
        cols += '<td><span class="align-middle"><i class="delete-row align-middle"/>' + backupSymbol + '</span></td>';
        cols += '<td style="display: none;"></td>';
        cols += '<td class="gene-error" colspan="2"></td>';
    } else {
        cols += '<td><span class="align-middle"><i class="delete-row align-middle"/><a href="https://www.ncbi.nlm.nih.gov/gene/' + gene.id + '" target="_blank" class="align-middle">' + gene.symbol + '</a></span></td>';
        cols += '<td style="display: none;">' + gene.id + '</td>';
        cols += '<td><span class="align-middle">' + gene.name + '</span></td>';
        cols += '<td class="text-center"><input class="align-middle" type="checkbox"/></td>';
    }
    newRow.append(cols);
    table.append(newRow);
}

function addTermRow(table, term, backupId) {
    var newRow = $("<tr class='new-row'></tr>");
    var cols = "";
    if ( term === null ) {
        console.log("Issue obtaining metadata for: " + backupId);
        cols += '<td><span class="align-middle"><i class="delete-row align-middle"/>' + backupId + '</span></td>';
        cols += '<td class="term-error" colspan="4"></td>';
    } else {
        cols += '<td><span class="align-middle">' +
            '<i class="delete-row"/>' +
            '<a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=' + term.id + '" ' +
            'target="_blank" data-toggle="tooltip" class="align-middle" title="' + term.definition +'">' + term.id + '</a></span></td>';
        cols += '<td><span class="align-middle">' + term.name + '</span></td>';
        cols += '<td><span class="align-middle">' + term.aspect + '</span></td>';
        cols += '<td><a href="#" class="align-middle overlap-show-modal" data-toggle="modal" data-target="#overlapModal">' + term.frequency + '</a></td>';
        cols += '<td><span class="align-middle">' + term.size + '</span></td>';
    }
    newRow.append(cols);
    table.append(newRow);
    newRow.find('[data-toggle="tooltip"]').tooltip()
}

$(document).ready(function () {

    var initialModel = collectModel();

    // Enable navigation prompt
    window.onbeforeunload = function() {
        return JSON.stringify(initialModel)!==JSON.stringify(collectModel()) ?  true : undefined;
    };

    $(document).on("click", ".save-model", function () {
        var model = collectModel();

        var spinner = $(this).find('.spinner');
        spinner.removeClass("d-none");

        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(model),
            contentType: "application/json",
            success: function(r) {
                $('.success-row').show();
                $('.error-row').hide();
                spinner.addClass("d-none");
                initialModel = collectModel();
            },
            error: function(r) {
                $('.success-row').hide();
                $('.error-row').show();
                spinner.addClass("d-none");
            }
        });
    });

    $(document).on("keypress", ".autocomplete", function (e) {
        if(e.which == 13) {
            $(this).closest('.input-group').find('button.add-row').click();
        }
    });

    // Genes

    $(function () {
        var cache = {};
        var autocomplete = $("#genes").find(".autocomplete");
        autocomplete.autocomplete({
            minLength: 2,
            delay: 500,
            source: function (request, response) {
                var term = request.term;
                if (term in cache) {
                    response(cache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }

                $.getJSON("/taxon/" + currentTaxon.id + "/gene/search/" + term + "?max=10", request, function (data, status, xhr) {

                    if (!data.length) {
                        data = [
                            {
                                noresults: true,
                                label: 'No matches found',
                                value: term
                            }
                        ];
                    }

                    cache[term] = data;
                    response(data);
                });
            },
            select: function( event, ui ) {
                autocomplete.val( ui.item.match.symbol );
                return false;
            }
        });
        autocomplete.autocomplete( "instance" )._renderItem = function( ul, item ) {
            return $( "<li>" )
                .append( "<div class='pl-3'><b>" + item.match.symbol + "</b>: " + item.match.name + " (<i>" + item.match.aliases + "</i>)</div>" )
                .appendTo( ul );
        };
        autocomplete.autocomplete( "instance" )._create = function() {
            this._super();
            this.widget().menu( "option", "items", "> :not(.ui-autocomplete-category)" );
        };
        autocomplete.autocomplete( "instance" )._renderMenu = function(ul, items) {
            var that = this,
                currentCategory = "";

            if(items.length === 1 && items[0].noresults ){
                ul.append( "<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #fddce5; font-size: 1rem;'>No Results</li>" );
                return;
            }

            $.each( items, function( index, item ) {
                var li;
                var label = item.matchType + " : " + item.match.symbol;
                if ( item.matchType !== currentCategory ) {
                    ul.append( "<li aria-label='" + label + "' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #e3f2fd; font-size: 1rem;'>" + item.matchType + "</li>" );
                    currentCategory = item.matchType;
                }
                li = that._renderItemData( ul, item );
                if ( item.matchType ) {
                    li.attr( "aria-label", label );
                }
            });
        };

    });

    $("#genes").on("click", ".add-row", function () {

        var line = $.trim($(this).closest('.input-group').find('input').val());

        var symbols = line.split(",").map(function(item) {
            return item.trim();
        });

        var self = $(this);
        var spinner = self.find('.spinner');
        spinner.removeClass("d-none");


        $.ajax({
            type: "POST",
            url: '/taxon/' + currentTaxon.id + '/gene/search',
            data: JSON.stringify(symbols),
            contentType: "application/json",
            success: function(data) {

                for (var symbol in data) {
                    if (data.hasOwnProperty(symbol)) {
                        addGeneRow(self.closest('table'), data[symbol], symbol);
                    }
                }
            }

        }).always(function() {
            spinner.addClass("d-none");
        });
        $(this).closest('.input-group').find('input').val("")
    });

    // Terms

    $(function () {
        var cache = {};
        var autocomplete = $("#terms").find(".autocomplete");
        autocomplete.autocomplete({
            minLength: 2,
            delay: 500,
            source: function (request, response) {
                var term = request.term;
                if (term in cache) {
                    response(cache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }
                $.getJSON("/taxon/" + currentTaxon.id + "/term/search/" + term + "?max=10", request, function (data, status, xhr) {

                    if (!data.length) {
                        data = [
                            {
                                noresults: true,
                                label: 'No matches found',
                                value: term
                            }
                        ];
                    }

                    cache[term] = data;
                    response(data);
                });
            },
            select: function( event, ui ) {
                autocomplete.val( ui.item.match.id );
                return false;
            }
        });
        autocomplete.autocomplete( "instance" )._renderItem = function( ul, item ) {
            return $( "<li>" )
                .append( "<div class='pl-3'><b>" + item.match.id + "</b>: " + item.match.name + " (Size: " + item.match.size + ")</div>" )
                .appendTo( ul );
        };
        autocomplete.autocomplete( "instance" )._create = function() {
            this._super();
            this.widget().menu( "option", "items", "> :not(.ui-autocomplete-category)" );
        };
        autocomplete.autocomplete( "instance" )._renderMenu = function(ul, items) {
            var that = this,
                currentCategory = "";

            if(items.length === 1 && items[0].noresults ){
                ul.append( "<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #fddce5; font-size: 1rem;'>No Results</li>" );
                return;
            }

            $.each( items, function( index, item ) {
                var li;
                var label = item.matchType + " : " + item.match.id;
                if ( item.matchType !== currentCategory ) {
                    ul.append( "<li aria-label='" + label + "' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #e3f2fd; font-size: 1rem;'>" + item.matchType + "</li>" );
                    currentCategory = item.matchType;
                }
                li = that._renderItemData( ul, item );
                if ( item.matchType ) {
                    li.attr( "aria-label", label );
                }
            });
        };

    });

    $("#terms").on("click", ".add-row", function () {

        var line = $.trim($(this).closest('.input-group').find('input').val());

        var ids = line.split(",").map(function(item) {
            return item.trim();
        });

        var self = $(this);

        var spinner = self.find('.spinner');
        spinner.removeClass("d-none");


        $.ajax({
            type: "POST",
            url: '/user/taxon/' + currentTaxon.id + '/term/search',
            data: JSON.stringify(ids),
            contentType: "application/json",
            success: function(data) {
                for (var goId in data) {
                    if (data.hasOwnProperty(goId)) {
                        addTermRow(self.closest('table'), data[goId], goId);
                    }
                }
            }

        }).always(function() {
            spinner.addClass("d-none");
        });
        $(this).closest('.input-group').find('input').val("")
    });

    $('.recommend-terms').click(function(e) {
        var spinner = $(this).find('.spinner');
        spinner.removeClass("d-none");
        $.getJSON("/user/taxon/" + currentTaxon.id + "/term/recommend?top=true", function (data, status, xhr) {
            try {
                var table = $('#terms').find('.term-table');
                $.each(data, function (index, item) {
                    addTermRow(table, item, "-")
                });
            } finally {
                spinner.addClass("d-none");
            }
        });
    });

    $('#overlapModal').on('show.bs.modal', function (e) {
        var goId = $(e.relatedTarget).closest('tr').find('td')[0].innerText;
        $("#overlapModal").find(".modal-body").load("/user/taxon/" + currentTaxon.id + "/term/" + goId + "/gene/view");
    })
});