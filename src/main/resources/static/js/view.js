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
        "order": [[2, "asc"], [0, "asc"]],
        "columnDefs": [
            {"name": "Symbol", "targets": 0},
            {"name": "Name", "targets": 1},
            {"name": "Tier", "targets": 2, "className": "text-center", "orderDataType": "dom-checkbox"}
        ],
        "footerCallback": function () {
            var api = this.api();
            var columnData = api.columns().data();
            var tiers = columnData[2];
            var counts = tiers.reduce(function (acc, curr) {
                if (acc[curr]) {
                    acc[curr]++;
                } else {
                    acc[curr] = 1;
                }
                return acc;
            }, {});
            $(api.column(1).footer()).html(
                '<span class="mx-1"><b>' + (counts.TIER1 ? counts.TIER1 : "0") + '</b> TIER1' + '</span>' +
                '<span class="mx-1" style="border-right: 3px solid #f2f7f9;"></span>' +
                '<span class="mx-1"><b>' + (counts.TIER2 ? counts.TIER2 : "0") + '</b> TIER2' + '</span>' +
                '<span class="mx-1" style="border-right: 3px solid #F2F7F9;"></span>' +
                '<span class="mx-1"><b>' + (counts.TIER3 ? counts.TIER3 : "0") + '</b> TIER3' + '</span>'
            );

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

    $('#overlapModal').on('show.bs.modal', function (e) {
        var taxonId = $(e.relatedTarget).closest('div.tab-pane')[0].id.split("-")[1];
        var goId = $(e.relatedTarget).closest('tr').find('td')[0].innerText;
        $("#overlapModal").find(".modal-body").load("/user/taxon/" + taxonId + "/term/" + goId + "/gene/view");
    });

    $('.terms-tab').on('shown.bs.tab', function () {
        $('.term-table').DataTable().draw();
    });
})();