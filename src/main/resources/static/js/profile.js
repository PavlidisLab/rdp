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

    // Publication Information
    var publications = [];
    $('#publication-table').find('tbody > tr').each(function(idx) {
        var tds = $(this).find('td');
        var a = $(tds[1]).find('a')[0];
        publications.push({
            pmid:  parseInt(tds[0].innerText, 10),
            title: a ? a.innerText : ""
        })
    });
    publications.sort(function(a, b){return b.pmid-a.pmid});
    profile.publications = publications;

    return profile;
}

$(document).ready(function () {

    // Create initial profile
    var initialProfile = collectProfile();

    // Enable navigation prompt
    window.onbeforeunload = function() {
        return JSON.stringify(initialProfile)!==JSON.stringify(collectProfile()) ?  true : undefined;
    };

    $(document).on("keypress", ".pub-input", function (e) {
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
        var metadata = {};
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

        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(profile),
            contentType: "application/json",
            success: function(r) {
                $('.success-row').show();
                $('.error-row').hide();
                spinner.addClass("d-none");
                initialProfile = collectProfile();
                $('.new-row').removeClass("new-row");
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

    $('#publication-table').DataTable({
        "scrollY":        "200px",
        "scrollCollapse": true,
        "paging":         false,
        "searching": false,
        "info": false,
        "order": [[ 0, "desc" ]]
    });

});
