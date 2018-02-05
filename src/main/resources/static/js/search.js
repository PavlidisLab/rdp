$(document).ready(function () {
    $("form").submit(function (event) {
        var tableContainer = $("#userTable");
        tableContainer.html($('<i class="mx-2 spinner"></i>'));
        tableContainer.load("/manager/search/view", $(this).serialize());

        event.preventDefault();
    });
});