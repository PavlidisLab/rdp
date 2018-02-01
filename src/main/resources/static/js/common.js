$(document).ready(function () {

    $(document).on("click", '.editable', function () {
        var row = $(this).closest(".edit-container").find(".data-edit");
        var state = row.prop('contenteditable') === 'true';
        row.prop('contenteditable', !state);
        $(this).toggleClass("saveable", !state);
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

    $('[data-toggle="tooltip"]').tooltip()

});
