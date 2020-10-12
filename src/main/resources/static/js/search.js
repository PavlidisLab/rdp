(function () {
    "use strict";

    function initializeUserPreviewPopover() {
        /* jshint validthis: true */
        var element = $(this);
        var userId = element.data('user-id');
        $.get('/search/view/user-preview/' + userId, function (data) {
            element.popover({
                container: 'body',
                trigger: 'hover tooltip',
                html: true,
                content: data
            });
        });
    }

    function checkItl(itlChbox) {
        if (itlChbox.prop("checked")) {
            $("#itlResults").show();
            $("[name=iSearch]").val(true);
        } else {
            $("#itlResults").hide();
            $('[name="iSearch"]').val(false);
        }
    }

    function checkTaxon(taxonSelect) {
        // noinspection EqualityComparisonWithCoercionJS // multi-browser support // Taxon also checked in SearchController.java
        if (taxonSelect.val() === "9606") {
            $("#ortholog-box").show();
        } else {
            $("#ortholog-box").hide();
        }
    }

    $("form.search").submit(function (event) {

        var formData = $(this).serialize();

        /* retrieve nearby organ systems */
        var organsForm = $(this).closest('.tab-pane').find('.organs-form').serialize();
        if (organsForm) {
            formData = formData + '&' + organsForm;
        }

        // Show search results
        var tableContainer = $("#userTable");
        tableContainer.html($('<i class="mx-2 spinner"></i>'));

        // noinspection JSUnusedLocalSymbols
        tableContainer.load("/search/view", formData, function (responseText, textStatus) {
            if (textStatus === "error") {
                tableContainer.html(responseText);
            }
            tableContainer.find('[data-toggle="tooltip"]').tooltip();
            tableContainer.find('.user-preview-popover').each(initializeUserPreviewPopover);
        });

        // Show orthologs
        $(".ortholog-search-text").html(""); // Clear existing results.
        if ($("#symbolInput").val() !== "" && $("#ortholog-box").is(":visible")) {
            var orthologContainer = $("#orthologsResults");
            orthologContainer.html($('<i class="mx-2 spinner"></i>'));
            orthologContainer.load("/search/view/orthologs", formData, function (responseText, textStatus) {
                if (textStatus === "error") {
                    /* display nothing if there's issue with orthologs */
                    orthologContainer.html('');
                }
                orthologContainer.find('[data-toggle="tooltip"]').tooltip();
                orthologContainer.find('.user-preview-popover').each(initializeUserPreviewPopover);
            });
        }

        // Show international search results
        if ($("#isearch-checkbox").is(":checked")) {
            var itlTableContainer = $("#itlUserTable");
            itlTableContainer.html($('<i class="mx-2 spinner"></i>'));
            // noinspection JSUnusedLocalSymbols
            itlTableContainer.load("/search/view/international", formData, function (responseText, textStatus, req) {
                if (textStatus === "error") {
                    itlTableContainer.html(responseText);
                }
                itlTableContainer.find('[data-toggle="tooltip"]').tooltip();
            });
        }

        event.preventDefault();
    });

    // International search property setting
    var itlChbox = $("#isearch-checkbox");
    checkItl(itlChbox);
    itlChbox.click(function () {
        checkItl(itlChbox);
    });

    // Ortholog selection show&hide
    var taxonSelect = $("#taxonId");
    checkTaxon(taxonSelect);
    taxonSelect.on("change", function () {
        checkTaxon(taxonSelect);
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
                $.getJSON("/taxon/" + taxonId + "/gene/search/" + term + "?max=10", request, function (data, status, xhr) {

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
            select: function (event, ui) {
                autocomplete.val(ui.item.match.symbol);
                return false;
            }
        });
        autocomplete.autocomplete("instance")._renderItem = function (ul, item) {
            return $("<li>")
                .append("<div class='pl-3'><b>" + item.match.symbol + "</b>: " + item.match.name + " (<i>" + item.match.aliases + "</i>)</div>")
                .appendTo(ul);
        };
        autocomplete.autocomplete("instance")._create = function () {
            this._super();
            this.widget().menu("option", "items", "> :not(.ui-autocomplete-category)");
        };
        autocomplete.autocomplete("instance")._renderMenu = function (ul, items) {
            var that = this,
                currentCategory = "";

            if (items.length === 1 && items[0].noresults) {
                ul.append("<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #fddce5; font-size: 1rem;'>No Results</li>");
                return;
            }

            $.each(items, function (index, item) {
                var li;
                var label = item.matchType + " : " + item.match.symbol;
                if (item.matchType !== currentCategory) {
                    ul.append("<li aria-label='" + label + "' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #e3f2fd; font-size: 1rem;'>" + item.matchType + "</li>");
                    currentCategory = item.matchType;
                }
                li = that._renderItemData(ul, item);
                if (item.matchType) {
                    li.attr("aria-label", label);
                }
            });
        };

        $('[name="nameLikeBtn"]').click(function () {
            $('[name="nameLikeBtn"]').toggleClass('active', false);
            $(this).toggleClass('active', true);
        });
    });

    /* initialize user preview popovers */
    $('.user-preview-popover').each(initializeUserPreviewPopover);
})();