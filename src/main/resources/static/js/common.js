var $ = require('jquery');
window.$ = window.jQuery = $;
require('jquery-ui/ui/widgets/autocomplete');
require('popper.js');
require('bootstrap');
require('bootstrap-select');
require('datatables.net');
require('datatables.net-bs4');

/* style */
require('bootstrap/dist/css/bootstrap.css');
require('open-iconic/font/css/open-iconic-bootstrap.css');
require('bootstrap-select/dist/css/bootstrap-select.css');
require('jquery-ui/themes/base/core.css');
require('jquery-ui/themes/base/menu.css');
require('jquery-ui/themes/base/autocomplete.css');
require('jquery-ui/themes/base/theme.css');
require('datatables.net-bs4/css/dataTables.bootstrap4.css');

require('../css/common.css');

(function () {
    "use strict";

    var $ = require('jquery');

    $(document).on("click", '.editable', function () {
        var inputs = $(this).closest(".edit-container").find(".data-edit");
        var disabled = inputs.prop('disabled');

        inputs.prop('disabled', !disabled);
        $(this).toggleClass("saveable", disabled);
        inputs.focus();

    });

    $(document).on("mousedown", '.editable', function (e) {
        e.preventDefault();
    });
    $(document).on("click", 'table.dataTable .delete-row', function () {
        var table = $(this).closest('table').DataTable();
        table.row($(this).closest('tr')).remove().draw();
    });

    // $(document).on("click", '.reset-table', function () {
    //     $(this).closest('table').find('tr.new-row').remove();
    // });

    $(document).on("click", ".close", function () {
        $(this).closest('.row').hide();
    });

    $(document).on("focusout", ".edit-container", function () {
        $(this).find('.data-edit').prop('disabled', true);
        $(this).find('.editable').removeClass("saveable");
    });

    $(document).on("keypress", "input.data-edit", function (e) {
        if (e.which === 13) {
            $(this).focusout();
        }
    });

    $(document).ready(function () {
        $('[data-toggle="tooltip"]').tooltip();

        /* we use a hide behaviour on alert instead of bootstrap defaults to remove the element from DOM */
        $('.alert .close').on('click', function () {
            $(this).parent().hide();
        });
    });
})();
