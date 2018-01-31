var currentTaxon = 9606;

function collectModel() {
    var model = {};

    // Research Information
    model.description = $.trim($('.research-info').find('.data-edit')[0].innerText);

    // Gene Information
    var geneTierMap = {};
    $('.gene-table').find('tbody > tr').each(function(idx) {
        var tds = $(this).find('td');

        // If id column contains an integer
        if (tds[1].innerText == parseInt(tds[1].innerText, 10) ) {
            geneTierMap[tds[1].innerText] = $(tds[3]).find('input[type=checkbox]').prop('checked') ? "TIER1" : "TIER2";
        }
    });
    model.geneTierMap = geneTierMap;

    return model;
}

$(document).ready(function () {

    var initialModel = collectModel();

    $(function () {
        var cache = {};
        var autocomplete = $("#genes").find(".autocomplete");
        autocomplete.autocomplete({
            minLength: 2,
            delay: 1000,
            source: function (request, response) {
                var term = request.term;
                if (term in cache) {
                    response(cache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }

                $.getJSON("/taxon/" + currentTaxon + "/gene/search/" + term + "?max=10", request, function (data, status, xhr) {
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

    $(document).on("keypress", ".autocomplete", function (e) {
        if(e.which == 13) {
            $(this).closest('.input-group').find('button').click();
        }
    });

    $("#genes").on("click", ".add-row", function () {

        var line = $.trim($(this).closest('.input-group').find('input').val());

        var symbols = line.split(",").map(function(item) {
            return item.trim();
        });

        var self = $(this);
        $.ajax({
            type: "POST",
            url: '/taxon/' + currentTaxon + '/gene/search',
            data: JSON.stringify(symbols),
            contentType: "application/json",
            success: function(data) {

                for (var symbol in data) {
                    if (data.hasOwnProperty(symbol)) {
                        var gene = data[symbol];
                        var newRow = $("<tr></tr>");
                        var cols = "";
                        if ( gene === null ) {
                            console.log("Issue obtaining metadata for: " + symbol);
                            cols += '<td><i class="delete-row"/>' + symbol + '</td>';
                            cols += '<td style="display: none;"></td>';
                            cols += '<td class="gene-error" colspan="2"></td>';
                        } else {
                            cols += '<td><i class="delete-row"/><a href="https://www.ncbi.nlm.nih.gov/gene/' + gene.id + '" target="_blank">' + gene.symbol + '</a></td>';
                            cols += '<td style="display: none;">' + gene.id + '</td>';
                            cols += '<td>' + gene.name + '</td>';
                            cols += '<td class="text-center"><input type="checkbox"/></td>';
                        }
                        newRow.append(cols);
                        self.closest('table').append(newRow);
                    }
                }
            }

        });
        $(this).closest('.input-group').find('input').val("")
    });

    $(document).on("click", ".save-model", function () {
        var model = collectModel();

        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(model),
            contentType: "application/json",
            success: function(r) {
                $('.success-row').show();
                $('.error-row').hide();
            },
            error: function(r) {
                $('.success-row').hide();
                $('.error-row').show();
            }
        });
    });
});