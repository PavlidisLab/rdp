function collectProfile() {
    var profile = {};

    // Basic Information
    var values = $('.basic-info').find('.data-edit');
    profile.name = $.trim(values[0].innerText);
    profile.lastName = $.trim(values[1].innerText);
    profile.organization = $.trim(values[2].innerText);
    profile.department = $.trim(values[3].innerText);
    profile.website = $.trim(values[4].innerText);

    // Contact Information
    profile.phone = $.trim($('.contact-info').find('.data-edit')[0].innerText);

    // Research Information
    profile.description = $.trim($('.research-info').find('.data-edit')[0].innerText);

    // Publication Information
    var publications = [];
    $('.publication-info').find('tbody > tr').each(function(idx) {
        var tds = $(this).find('td');
        var a = $(tds[1]).find('a')[0];
        publications.push({
            pmid:  parseInt(tds[0].innerText, 10),
            url: a ? a.href : "",
            title: a ? a.innerText : ""
        })
    });
    profile.publications = publications;

    return profile;
}

$(document).ready(function () {

    // Create initial profile
    var initialProfile = collectProfile();

    $(document).on("click", ".add-row", function () {
        var newRow = $("<tr></tr>");
        var cols = "";

        var pubmed = $.trim($(this).closest('.input-group').find('input').val());

        if (pubmed != parseInt(pubmed, 10)) {
            return false;
        }

        cols += '<td><i class="delete-row"/>' + pubmed + '</td>';
        var col = '<td class="pubmed-error"></td>';
        var self = $(this);
        $.get('https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pmc&amp;id='+pubmed+'&amp;retmode=json', function(data) {
            try {

                var title = data['result'][pubmed]['title'];

                var doi = '';
                data['result'][pubmed]['articleids'].forEach(function (aid) {
                    if (aid['idtype'] === "doi") {
                        doi = aid['value'];
                    }
                });

                col = '<td><a href="https://doi.org/' + doi + '" target="_blank">' + title + '</a></td>';

            } catch (e) {
                console.log("Issue obtaining metadata for: " + pubmed);
            }
            cols += col;

            newRow.append(cols);
            self.closest('table').append(newRow);
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
            },
            error: function(r) {
                $('.success-row').hide();
                $('.error-row').show();
                spinner.addClass("d-none");
            }
        });
    });

});
