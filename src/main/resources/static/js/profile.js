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
        "order": [[0, "asc"]]
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

        var table = publicationTable.DataTable();

        // Try to get metadata for the articles
        var rows = [];
        $.get('https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&amp;id=' + encodeURIComponent(ids.join(",")) + '&amp;retmode=json', function (data) {
            $.each(ids, function (idx, pubmed) {
                /* check if it's already in the table */
                var val = '<i class="delete-row"></i> ' + pubmed;
                if (table.column(0).data().indexOf(val) !== -1) {
                    return;
                }
                var row = [];
                row.push(val);
                try {
                    var title = data.result[pubmed].title;
                    title = title.length > 100 ?
                        title.substring(0, 100 - 3) + "..." :
                        title;

                    row.push('<a href="https://www.ncbi.nlm.nih.gov/pubmed/' + encodeURIComponent(pubmed) + '" target="_blank" rel="noopener">' + (title ? title : 'Unknown Title') + '</a>');
                } catch (e) {
                    window.console.log("Issue obtaining metadata for: " + pubmed, e);
                }
                rows.push(row);
            });
        }).done(function () {
            table.rows.add(rows).draw().nodes()
                .to$()
                .addClass('new-row');
        });

    });

    $(document).on("click", ".save-profile", function () {
        var profile = collectProfile();

        var spinner = $(this).find('.spinner');
        spinner.toggleClass('d-none', false);

        // reset any invalid state
        $('.is-invalid').toggleClass('is-invalid', false);
        $('.invalid-feedback').remove();

        // noinspection JSUnusedLocalSymbols
        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(profile),
            contentType: "application/json"
        }).done(function (r) {
            // update the initial profile to the newly saved one
            initialProfile = profile;
            $('#profile-success-alert-message').text(r.message);
            $('#profile-success-alert').show();
            $('#profile-error-alert').hide();
            // display the verified badge if the email is verified
            // hide the not verified badge and resend link if the email is verified
            // sorry for the inversed logic here, the d-none class hides the tag
            $('#contact-email-verified-badge').toggleClass('d-none', !r.contactEmailVerified);
            $('#contact-email-not-verified-badge').toggleClass('d-none', r.contactEmailVerified);
            $('#contact-email-resend-verification-email-button').toggleClass('d-none', r.contactEmailVerified);
            $('#saved-button').focus();
        }).fail(function (r) {
            var message = "Your profile could not be saved.";
            if ('fieldErrors' in r.responseJSON) {
                r.responseJSON.fieldErrors.forEach(function (fieldError) {
                    $('[name="' + fieldError.field + '"]')
                        .toggleClass('is-invalid', true)
                        .after($('<div/>', {'class': 'invalid-feedback text-danger d-block'}).text(fieldError.message));
                });
            }
            $('#profile-error-alert-message').html(message);
            $('#profile-error-alert').show();
            $('#profile-success-alert').hide();
        }).always(function () {
            spinner.toggleClass('d-none', true);
        });
    });
})();
