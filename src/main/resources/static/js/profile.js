function collectProfile() {
    var profile = {};

    // Basic Information
    var values = $('.basic-info').find('.data-edit');
    profile.name = $.trim(values[0].value);
    profile.lastName = $.trim(values[1].value);
    profile.organization = $.trim(values[2].value);
    profile.department = $.trim(values[3].value);
    profile.website = $.trim(values[4].value);

    // Contact Information
    profile.phone = $.trim($('.contact-info').find('.data-edit')[0].value);

    // Research Information
    profile.description = $.trim($('.research-info').find('.data-edit')[0].value);

    profile.privacyLevel = $('input[name=privacy]:checked').val();
    profile.shared = $('#privacy-sharing-checkbox').prop('checked');
    profile.hideGenelist = $('#privacy-genelist-checkbox').prop('checked');

    // Publication Information
    var publications = [];
    // noinspection JSUnusedLocalSymbols
    $('#publication-table').DataTable().rows().every( function ( rowIdx, tableLoop, rowLoop ) {
        var node = $(this.node());
        var pmidstr = node.find('td')[0].innerText;
        var pmid =  parseInt(pmidstr, 10);

        // noinspection EqualityComparisonWithCoercionJS
        if (pmidstr == pmid ) {
            var a = node.find('a')[0];
            publications.push({
                pmid:  pmid,
                title: a ? a.innerText : ""
            });
        }
    } );
    publications.sort(function(a, b){return b.pmid-a.pmid});
    profile.publications = publications;

    return profile;
}

$(document).ready(function () {

    // auto-hide save message
    $('#saved-button').blur(function(){
        console.log( "Handler for .blur() called." );
        $('.success-row').hide();
    });

    $('#publication-table').DataTable({
        "scrollY":        "200px",
        "scrollCollapse": true,
        "paging":         false,
        "searching": false,
        "info": false,
        "order": [[ 0, "desc" ]]
    });

    // Create initial profile
    var initialProfile = collectProfile();

    // Enable navigation prompt
    window.onbeforeunload = function() {
        return JSON.stringify(initialProfile)!==JSON.stringify(collectProfile()) ?  true : undefined;
    };

    // Auto-check international sharing with public privacy setting
    var itlChbox = $("#privacy-sharing-checkbox");
    var origItlState = itlChbox.is(":checked");
    if($("#privacyLevelPublic").is(":checked")){
        itlChbox.prop('checked', true);
        itlChbox.prop('readonly', true);
        itlChbox.prop('disabled', true);
    }
    $("input[name='privacy']").click(function () {
        if($("#privacyLevelPublic").is(":checked")){
            itlChbox.prop('checked', true);
            itlChbox.prop('readonly', true);
            itlChbox.prop('disabled', true);
        }else{
            itlChbox.prop('checked', origItlState);
            itlChbox.prop('readonly', false);
            itlChbox.prop('disabled', false);
        }
    });
    itlChbox.click(function(){
       origItlState = itlChbox.is(":checked");
    });

    $(document).on("keypress", ".pub-input", function (e) {
        // noinspection EqualityComparisonWithCoercionJS
        if(e.which == 13) {
            $(this).closest('.input-group').find('button.add-row').click();
        }
    });


    $(document).on("click", ".add-row", function () {
        var line = $.trim($(this).closest('.input-group').find('input').val());

        var ids = line.split(",").map(function(item) {
            return item.trim();
        }).filter(function(item) {
            return item;
        });

        // Try to get metadata for the articles
        var rows = [];
        $.get('https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&amp;id=' + ids.join(",") + '&amp;retmode=json', function(data) {
            $.each(ids, function(idx, pubmed) {

                var row = [];

                row.push('<i class="delete-row"></i>' + pubmed);

                try {
                    var title = data['result'][pubmed]['title'];
                } catch (e) {
                    console.log("Issue obtaining metadata for: " + pubmed, e);
                }

                title = title.length > 100 ?
                    title.substring(0, 100 - 3) + "..." :
                    title;

                row.push('<a href="https://www.ncbi.nlm.nih.gov/pubmed/' + pubmed + '" target="_blank">' + (title ? title : 'Unknown Title') + '</a>');

                rows.push(row);
            });
        }).done(function() {

            var table = $('#publication-table').DataTable();

            table.rows.add(rows).draw().nodes()
                .to$()
                .addClass( 'new-row' );
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
            success: function(r) {
                console.log('Yep!');
                $('.success-row').show();
                $('.error-row').hide();
                spinner.addClass("d-none");
                initialProfile = collectProfile();
                $('.new-row').removeClass("new-row");
                $('#saved-button').focus();
            },
            error: function(r) {
                var errorMessages = [];
                try {
                    $.each(r.responseJSON.errors, function(idx, item) {
                        errorMessages.push(item.field + " - " + item.defaultMessage)
                    })
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

});
