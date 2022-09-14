var formUtil = require('./util/form');

(function () {
    "use strict";

    function initializeUserPreviewPopover() {
        /* jshint validthis: true */
        var element = $(this);
        var userId = element.data('user-id');
        var anonymousUserId = element.data('anonymous-user-id');
        var remoteHost = element.data('remote-host');
        var popoverUrl = window.contextPath + '/search/view/user-preview/';
        if (anonymousUserId) {
            popoverUrl += 'by-anonymous-id/' + encodeURIComponent(anonymousUserId);
        } else {
            popoverUrl += encodeURIComponent(userId);
        }
        $.get(popoverUrl, remoteHost ? {'remoteHost': remoteHost} : {})
            .done(function (data, textStatus, jqXHR) {
                /* handle no content status code when the popover is empty */
                if (jqXHR.status === 204) {
                    return;
                }
                element.popover({
                    container: 'body',
                    trigger: 'hover tooltip',
                    html: true,
                    content: data
                });
            })
            .fail(function (data) {
                element.popover({
                    container: 'body',
                    trigger: 'hover tooltip',
                    html: true,
                    content: data
                });
            });
    }

    function checkTaxon(taxonSelect) {
        // noinspection EqualityComparisonWithCoercionJS // multi-browser support // Taxon also checked in SearchController.java
        if (taxonSelect.val() === "9606") {
            $("#ortholog-box").show();
        } else {
            $("#ortholog-box").hide();
        }
    }

    var tableContainer = $("#userTable");
    var searchSummary = $('#searchSummary');
    var orthologContainer = $("#orthologsResults");
    var itlTableContainer = $("#itlUserTable");
    var results = document.getElementById('results');
    var itlResults = document.getElementById("itlResults");

    // hide results with switching tab
    $('.search-mode-link[data-toggle=tab]').on('hide.bs.tab', function () {
        searchSummary.empty();
        orthologContainer.empty();
        results.classList.toggle('d-none', true);
        itlResults.classList.toggle('d-none', true);
    });

    $("form.search").submit(function (event) {

        var formData = formUtil.serialize(this);

        // ensure that iSearch is always part of the request parameters
        // this could also be done using a hidden input with the '_' prefix, but we don't want to contaminate the search
        // query syntax
        var iSearch = $(this).find('input[name="iSearch"]').is(':checked');
        if (!iSearch) {
            formData += '&iSearch=false';
        }

        // Show search results
        tableContainer.html($('<i class="mx-2 spinner"></i>'));
        tableContainer.load(window.contextPath + "/search/view", formData, function (responseText, textStatus) {
            if (textStatus === "error") {
                tableContainer.html(responseText);
            } else {
                tableContainer.find('[data-toggle="tooltip"]').tooltip();
                tableContainer.find('.user-preview-popover').each(initializeUserPreviewPopover);
            }
        });

        // show search summary
        searchSummary.html($('<i class="mx-2 spinner"></i>'));
        searchSummary.load(window.contextPath + '/search/view', formData + '&summarize=true', function (responseText, textStatus) {
            if (textStatus === "error") {
                searchSummary.html(''); // the error will be displayed in the main search view
            }
        });

        // Show orthologs
        if ($("#symbolInput").val() !== "" && $("#ortholog-box").is(":visible")) {
            orthologContainer.html($('<i class="mx-2 spinner"></i>'));
            orthologContainer.load(window.contextPath + "/search/view/orthologs", formData, function (responseText, textStatus) {
                if (textStatus === "error") {
                    orthologContainer.html(responseText);
                }
            });
        } else {
            orthologContainer.empty();
        }

        // show search results
        results.classList.toggle('d-none', false);

        // Show international search results
        if (iSearch) {
            itlResults.classList.toggle('d-none', false);
            itlTableContainer.html($('<i class="mx-2 spinner"></i>'));
            // noinspection JSUnusedLocalSymbols
            itlTableContainer.load(window.contextPath + "/search/view/international", formData, function (responseText, textStatus, req) {
                if (textStatus === "error") {
                    itlTableContainer.html(responseText);
                } else {
                    itlTableContainer.find('[data-toggle="tooltip"]').tooltip();
                    itlTableContainer.find('.user-preview-popover').each(initializeUserPreviewPopover);
                }
            });
        } else {
            itlResults.classList.toggle('d-none', true);
        }

        // update history stack
        window.history.pushState({}, '', '?' + formData);

        event.preventDefault();
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

                if (term.indexOf(",") !== -1) {
                    return;
                }

                // noinspection JSUnusedLocalSymbols
                $.getJSON(window.contextPath + "/taxon/" + encodeURIComponent(taxonId) + "/gene/search", {
                    query: term,
                    max: 10
                }).done(function (data, status, xhr) {
                    if (!data.length) {
                        data = [
                            {
                                noresults: true,
                                label: 'No matches found for "' + term + '".',
                                value: term
                            }
                        ];
                    }
                    taxonCache[term] = data;
                    response(data);
                }).fail(function () {
                    response([{noresults: true, label: 'Error querying search endpoint.', value: term}]);
                });
            },
            select: function (event, ui) {
                $(this).val(ui.item.label);
                return false;
            }
        });
    });

    $('[name="nameLikeBtn"]').click(function () {
        $('[name="nameLikeBtn"]').toggleClass('active', false);
        $(this).toggleClass('active', true);
    });

    $('.ontology-term-autocomplete').autocomplete({
        minLength: 2,
        delay: 200,
        source: function (request, response) {
            var term = request.term.trim();
            var ontologyId = $(this.element).data('ontologyId');
            response([{label: 'Loading results for "' + term + '"...'}]);
            $.getJSON(window.contextPath + '/search/ontology-terms/autocomplete', {
                query: term,
                ontologyId: ontologyId
            }).done(function (data) {
                if (!data.length) {
                    return response([{
                        noresults: true,
                        label: 'No matches found for "' + term + '".',
                        value: term
                    }
                    ]);
                } else {
                    response(data);
                }
            }).fail(function () {
                response([{noresults: true, label: 'Error querying search endpoint.', value: term}]);
            });
        },
        /**
         *
         * @param event
         * @param ui {{item: SearchResult}}
         * @returns {boolean}
         */
        select: function (event, ui) {
            // add the term ID to the selection
            $('<span class="badge badge-primary d-inline-flex align-items-center mr-1 mb-1">')
                .append($('<span>').text(ui.item.description))
                .append($('<button class="close" type="button">').text('×'))
                .append($('<input name="ontologyTermIds" type="hidden">').val(ui.item.id))
                .insertBefore($(this));
            $(this).val('');
            return false;
        }
    });

    /* initialize user preview popovers */
    $('.user-preview-popover').each(initializeUserPreviewPopover);
})();