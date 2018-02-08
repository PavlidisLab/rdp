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

        var table = $(this).closest('table');

        $.each(ids, function (index, pubmed) {
            if (pubmed != parseInt(pubmed, 10)) {
                return true;
            }
            var newRow = $("<tr class='new-row'></tr>");
            var cols = "";
            cols += '<td><i class="delete-row"/>' + pubmed + '</td>';

            newRow.append(cols);
            table.append(newRow);

        });

        // var col = '<td class="pubmed-error"></td>';
        // var self = $(this);
        // $.get('https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pmc&amp;id='+pubmed+'&amp;retmode=json', function(data) {
        //     try {
        //
        //         var title = data['result'][pubmed]['title'];
        //
        //         var doi = '';
        //         data['result'][pubmed]['articleids'].forEach(function (aid) {
        //             if (aid['idtype'] === "doi") {
        //                 doi = aid['value'];
        //             }
        //         });
        //
        //         col = '<td><a href="https://doi.org/' + doi + '" target="_blank">' + title + '</a></td>';
        //
        //     } catch (e) {
        //         console.log("Issue obtaining metadata for: " + pubmed);
        //     }
        //     cols += col;
        //
        //     newRow.append(cols);
        //     self.closest('table').append(newRow);
        // });
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
                $('.success-row').hide();
                $('.error-row').show();
                spinner.addClass("d-none");
            }
        });
    });

});
