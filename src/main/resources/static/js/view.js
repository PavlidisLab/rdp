$(document).ready(function () {

    $('#publication-table').DataTable({
        "scrollY":        "200px",
        "scrollCollapse": true,
        "paging":         false,
        "searching": false,
        "info": false,
        "order": [[ 0, "desc" ]]
    });

    $('.gene-table').DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[2, "asc"], [0, "desc"]],
        "columnDefs": [
            {"name": "Symbol", "targets": 0},
            {"name": "Name", "targets": 1},
            {"name": "Primary", "targets": 2, "className": "text-center", "orderDataType": "dom-checkbox"}
        ],
        "footerCallback": function (row, data, start, end, display) {
            var api = this.api();
            var columnData = api.columns().data();
            var tiers = columnData[columnData.length - 1];
            var counts = tiers.reduce(function (acc, curr) {
                acc[curr] ? acc[curr]++ : acc[curr] = 1;
                return acc;
            }, {});
            $(api.column(1).footer()).html(
                "<b>" + (counts['TIER1'] ? counts['TIER1'] : "0") + "</b> TIER1 <span style='margin: 1em;border-left: 3px solid #F2F7F9;'/> " +
                "<b>" + (counts['TIER2'] ? counts['TIER2'] : "0") + "</b> TIER2 <span style='margin: 1em;border-left: 3px solid #F2F7F9;'/> " +
                "<b>" + (counts['TIER3'] ? counts['TIER3'] : "0") + "</b> TIER3"
            );

        }
    });

    $('.term-table').DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "desc"]]
    });

    $('#overlapModal').on('show.bs.modal', function (e) {

        var taxon_id = $(e.relatedTarget).closest('div.tab-pane')[0].id.split("-")[1];
        console.log(taxon_id);
        var goId = $(e.relatedTarget).closest('tr').find('td')[0].innerText;
        $("#overlapModal").find(".modal-body").load("/user/taxon/" + taxon_id + "/term/" + goId + "/gene/view");
    });

    $('.terms-tab').on('shown.bs.tab', function (e) {
        $('.term-table').DataTable().draw();
    })
});