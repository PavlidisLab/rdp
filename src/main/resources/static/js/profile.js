(function () {
    "use strict";

    var publicationTable = $('#publication-table');

    function collectProfile() {
        var profile = {};

        // Basic Information
        profile.name = $.trim($('[name="profile.name"]').val());
        profile.lastName = $.trim($('[name="profile.lastName"]').val());
        profile.organization = $.trim($('[name="profile.organization"]').val());
        profile.department = $.trim($('[name="profile.department"]').val());
        profile.website = $.trim($('[name="profile.website"]').val());

        // we handle empty string, null and undefined value (if researcher type feature is not enabled)
        profile.researcherPosition = $('[name="researcherPosition"]:checked').val() || null;

        // here an empty list is a valid selection, so we only need to convert undefined when the field is not displayed
        profile.researcherCategories = $('[name="researcherCategories"]').val();
        if (profile.researcherCategories === undefined) {
            profile.researcherCategories = null;
        }

        // Contact Information
        profile.contactEmail = $.trim($('[name="profile.contactEmail"]').val());
        profile.phone = $.trim($('[name="profile.phone"]').val());

        // Research Information
        profile.description = $.trim($('[name="profile.description"]').val());

        profile.privacyLevel = parseInt($('input[name=privacyLevel]:checked').val());
        profile.shared = $('[name="profile.shared"]').prop('checked');
        profile.hideGenelist = $('[name="profile.hideGenelist"]').prop('checked');

        // Publication Information
        var publications = [];
        // noinspection JSUnusedLocalSymbols
        publicationTable.DataTable().rows().every(function (rowIdx, tableLoop, rowLoop) {
            var node = $(this.node());
            var pubmedId = parseInt(node.find('td')[0].innerText, 10);

            // noinspection EqualityComparisonWithCoercionJS
            if (!isNaN(pubmedId)) {
                var a = node.find('a')[0];
                publications.push({
                    pmid: pubmedId,
                    title: a ? a.innerText : ""
                });
            }
        });
        publications.sort(function (a, b) {
            return b.pmid - a.pmid;
        });
        profile.publications = publications;

        var organUberonIds = $('[name="organUberonIds"]:checked')
            .map(function (i, elem) {
                return elem.value;
            }).get();

        return {'profile': profile, 'organUberonIds': organUberonIds};
    }

    // auto-hide save message
    $('#saved-button').blur(function () {
        window.console.log("Handler for .blur() called.");
        $('.success-row').hide();
    });

    publicationTable.DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "desc"]]
    });

    // Create initial profile
    var initialProfile = collectProfile();

    // Enable navigation prompt
    window.onbeforeunload = function () {
        return JSON.stringify(initialProfile) !== JSON.stringify(collectProfile()) ? true : undefined;
    };

    // Auto-check international sharing with public privacy setting
    var itlChbox = $("#privacy-sharing-checkbox");
    var origItlState = itlChbox.is(":checked");
    if ($("#privacyLevelPublic").is(":checked")) {
        itlChbox.prop('checked', true);
        itlChbox.prop('readonly', true);
        itlChbox.prop('disabled', true);
    }
    $("input[name='privacy']").click(function () {
        if ($("#privacyLevelPublic").is(":checked")) {
            itlChbox.prop('checked', true);
            itlChbox.prop('readonly', true);
            itlChbox.prop('disabled', true);
        } else {
            itlChbox.prop('checked', origItlState);
            itlChbox.prop('readonly', false);
            itlChbox.prop('disabled', false);
        }
    });
    itlChbox.click(function () {
        origItlState = itlChbox.is(":checked");
    });

    $(document).on("keypress", ".pub-input", function (e) {
        // noinspection EqualityComparisonWithCoercionJS
        if (e.which === 13) {
            $(this).closest('.input-group').find('button.add-row').click();
        }
    });


    $(document).on("click", ".add-row", function () {
        var line = $.trim($(this).closest('.input-group').find('input').val());

        var ids = line.split(",").map(function (item) {
            return item.trim();
        }).filter(function (item) {
            return item;
        });

        // Try to get metadata for the articles
        var rows = [];
        $.get('https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&amp;id=' + ids.join(",") + '&amp;retmode=json', function (data) {
            $.each(ids, function (idx, pubmed) {

                var row = [];

                row.push('<i class="delete-row"></i>' + pubmed);

                try {
                    var title = data.result[pubmed].title;
                    title = title.length > 100 ?
                        title.substring(0, 100 - 3) + "..." :
                        title;

                    row.push('<a href="https://www.ncbi.nlm.nih.gov/pubmed/' + pubmed + '" target="_blank" rel="noopener">' + (title ? title : 'Unknown Title') + '</a>');
                } catch (e) {
                    window.console.log("Issue obtaining metadata for: " + pubmed, e);
                }

                rows.push(row);
            });
        }).done(function () {

            var table = publicationTable.DataTable();

            table.rows.add(rows).draw().nodes()
                .to$()
                .addClass('new-row');
        });

    });

    $(document).on("click", ".save-profile", function () {
        var profile = collectProfile();

        var spinner = $(this).find('.spinner');
        spinner.removeClass("d-none");

        // noinspection JSUnusedLocalSymbols
        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(profile),
            contentType: "application/json",
            success: function (r) {
                $('.success-row').show();
                $('.error-row').hide();
                spinner.addClass("d-none");
                initialProfile = collectProfile();
                $('.new-row').removeClass("new-row");
                $('#saved-button').focus();
                // hide the verification badge
                $('.contact-email-verification-badge').toggleClass('d-none', true);
            },
            error: function (r) {
                var errorMessages = [];
                try {
                    $.each(r.responseJSON.errors, function (idx, item) {
                        errorMessages.push(item.field + " - " + item.defaultMessage);
                    });
                } finally {
                    var message = "Profile Not Saved: " + errorMessages.join(", ");

                    $('.success-row').hide();
                    var $error = $('.error-row');
                    $error.find('div.alert').text(message);
                    $error.show();
                    spinner.addClass("d-none");
                }
            }
        });
    });
})();
