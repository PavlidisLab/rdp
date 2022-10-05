(function () {
    "use strict";

    $('#publication-table').DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "asc"]]
    });

    $('.gene-table').DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[3, "asc"], [1, "asc"]],
        "columnDefs": [
            {name: "Gene ID", targets: 0, visible: false},
            {
                name: "Symbol",
                targets: 1,
                render: function (data, type, row) {
                    var geneId = row[0];
                    return $('<a/>')
                        .attr('href', 'https://www.ncbi.nlm.nih.gov/gene/' + encodeURIComponent(geneId))
                        .attr('target', '_blank')
                        .attr('rel', 'noopener')
                        .text(data)[0].outerHTML;
                }
            },
            {name: "Name", "targets": 2},
            {name: "Tier", "targets": 3, "className": "text-center", "orderDataType": "dom-checkbox"},
            {targets: 4, visible: false}
        ],
        "footerCallback": function () {
            var api = this.api();
            var tiers = api.column(3).data();
            var counts = tiers.reduce(function (acc, curr) {
                acc[curr]++;
                return acc;
            }, {TIER1: 0, TIER2: 0, TIER3: 0});
            var footer = api.column(1).footer();
            footer.innerHTML =
                '<span><b>' + counts.TIER1 + '</b> Tier 1' + '</span>' +
                '<span class="mx-2 border-right"></span>' +
                '<span><b>' + counts.TIER2 + '</b> Tier 2' + '</span>' +
                '<span class="mx-2 border-right"></span>' +
                '<span><b>' + counts.TIER3 + '</b> Tier 3' + '</span>';
        }
    });

    $('.term-table').DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "asc"]]
    });

    $('.ontology-term-table').DataTable({
        paging: false,
        searching: false,
        info: false,
        order: [[0, "asc"]],
        columnDefs: [
            {
                targets: [2],
                render: function (data) {
                    return $('<span class="ontology-term-definition">').text(data)[0].outerHTML;
                }
            }
        ]
    });

    $('#overlapModal').on('show.bs.modal', function (e) {
        var taxonId = $(e.relatedTarget).closest('div.tab-pane')[0].id.split("-")[1];
        var goId = $(e.relatedTarget).closest('tr').find('td')[0].innerText;
        $("#overlapModal").find(".modal-body").load("/user/taxon/" + taxonId + "/term/" + goId + "/gene/view");
    });

    $('.terms-tab').on('shown.bs.tab', function () {
        $('.term-table').DataTable().draw();
    });
})();