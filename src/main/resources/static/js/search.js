(function () {
    "use strict";

    function initializeUserPreviewPopover() {
        /* jshint validthis: true */
        var element = $(this);
        var userId = element.data('user-id');
        var anonymousUserId = element.data('anonymous-user-id');
        var remoteHost = element.data('remote-host');
        var popoverUrl = '/search/view/user-preview/';
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
                itlTableContainer.find('.user-preview-popover').each(initializeUserPreviewPopover);
            });
        }

        // update history stack
        window.history.pushState({}, '', '?' + formData);

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

                if (term.indexOf(",") !== -1) {
                    return;
                }

                // noinspection JSUnusedLocalSymbols
                $.getJSON("/taxon/" + encodeURIComponent(taxonId) + "/gene/search", {query: term, max: 10})
                    .done(function (data, status, xhr) {
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
                    })
                    .fail(function () {
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

    $('.term-autocomplete').autocomplete({
        minLength: 2,
        delay: 300,
        source: function (request, response) {
            var term = request.term;
            var offset;
            if ((offset = term.lastIndexOf(',')) !== -1) {
                term = term.substring(offset + 1, term.length);
            }
            term = term.trim();
            $.getJSON('/search/ontology-terms/autocomplete', {query: term}).done(function (data) {
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
        focus: function () {
            // prevent value inserted on focus
            return false;
        },
        select: function (event, ui) {
            var term = $(this).val();
            var offset;
            if ((offset = term.lastIndexOf(',')) !== -1) {
                $(this).val(term.substring(0, offset) + ', ' + ui.item.label + ', ');
            } else {
                $(this).val(ui.item.label + ', ');
            }
            // add the term ID to the selection
            $('<input name="ontologyTermIds" type="hidden">')
                .val(ui.item.id)
                .insertAfter($(this));
            return false;
        }
    });

    /* initialize user preview popovers */
    $('.user-preview-popover').each(initializeUserPreviewPopover);
})();