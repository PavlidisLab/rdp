$(document).ready(function () {

    $(document).on("click", '.editable', function (e) {
        var inputs = $(this).closest(".edit-container").find(".data-edit");
        var disabled = inputs.prop('disabled');

        inputs.prop('disabled', !disabled);
        $(this).toggleClass("saveable", disabled);
        inputs.focus();

    });

    $(document).on("mousedown", '.editable', function (e) {
        e.preventDefault();
    });
    $(document).on("click", '.delete-row', function () {
        $(this).closest('tr').remove();
    });

    $(document).on("click", '.reset-table', function () {
        $(this).closest('table').find('tr.new-row').remove();
    });

    $(document).on("click", ".close", function () {
        $(this).closest('.row').hide();
    });

    $('[data-toggle="tooltip"]').tooltip();

    $(document).on("focusout", ".edit-container", function (e) {
        $(this).find('.data-edit').prop('disabled', true);
        $(this).find('.editable').removeClass("saveable");
    });

    $(document).on("keypress", "input.data-edit", function (e) {
        if(e.which == 13) {
            $(this).focusout();
        }
    });

});
