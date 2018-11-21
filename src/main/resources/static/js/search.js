function checkAndHide(itlChbox){
    if(itlChbox.prop("checked")){
        $("#itlResults").show();
        $("[name=iSearch]").val(true);
    }else{
        $("#itlResults").hide();
        $('[name="iSearch"]').val(false);
    }
}

$(document).ready(function () {
    $("form").submit(function (event) {
        var tableContainer = $("#userTable");
        tableContainer.html($('<i class="mx-2 spinner"></i>'));

        // noinspection JSUnusedLocalSymbols
        tableContainer.load("/manager/search/view", $(this).serialize(), function(responseText, textStatus, req) {
            if (textStatus !== "success") {
                tableContainer.html($('<span class="mx-2 text-danger">Something went wrong! Please try again.</span>'));
            }
        });

        if($("#isearch-checkbox").is(":checked")){
            var itlTableContainer = $("#itlUserTable");
            itlTableContainer.html($('<i class="mx-2 spinner"></i>'));
            // noinspection JSUnusedLocalSymbols
            itlTableContainer.load("/manager/search/view/international", $(this).serialize(), function(responseText, textStatus, req) {
                if (textStatus !== "success") {
                    itlTableContainer.html($('<span class="mx-2 text-danger">Something went wrong! Please try again.</span>'));
                }
            });
        }

        event.preventDefault();
    });

    var itlChbox = $("#isearch-checkbox");
    checkAndHide(itlChbox);
    itlChbox.click(function(){
        checkAndHide(itlChbox);
    });

    $(function () {
        var cache = {};
        var autocomplete = $(".gene-autocomplete");
        var taxonSelect = autocomplete.closest('form').find('select[name=taxonId]');
        autocomplete.autocomplete({
            minLength: 2,
            delay: 500,
            source: function (request, response) {
                var term = request.term;

                var taxonId = taxonSelect.val();
                if (!(taxonId in cache)) {
                    cache[taxonId] = {};
                }

                var taxonCache = cache[taxonId];

                if (term in taxonCache) {
                    response(taxonCache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }

                // noinspection JSUnusedLocalSymbols
                $.getJSON("/taxon/" + taxonId  + "/gene/search/" + term + "?max=10", request, function (data, status, xhr) {

                    if (!data.length) {
                        data = [
                            {
                                noresults: true,
                                label: 'No matches found',
                                value: term
                            }
                        ];
                    }

                    taxonCache[term] = data;
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
});