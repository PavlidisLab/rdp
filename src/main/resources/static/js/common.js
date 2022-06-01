(function () {
    "use strict";

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

    /* we use a hide behaviour on alert instead of bootstrap defaults to remove the element from DOM */
    $('.alert .close').on('click', function () {
        $(this).parent().hide();
    });

    $('[data-toggle="tooltip"]').tooltip();

    /**
     *
     * @param {Number} id
     * @param {String} label
     * @param {String} description
     * @param {String?} extras
     * @param {String} matchType
     * @param {*} match
     * @param {Number} score
     * @param {Object} attributes
     * @constructor
     */
    function SearchResult(id, label, description, extras, matchType, match, score, attributes) {
        this.noresults = false;
        this.id = id;
        this.label = label;
        this.description = description;
        this.extras = extras;
        this.matchType = matchType;
        this.match = match;
        this.score = score;
        this.attributes = attributes;
    }

    /* autocomplete look & feel based on SearchResult */
    $.widget('ui.autocomplete', $.ui.autocomplete, {
        _create: function () {
            this._super();
            this.widget().menu("option", "items", "> :not(.ui-autocomplete-category)");
        },
        /**
         * @param ul
         * @param {SearchResult} item
         * @returns {*|jQuery}
         */
        _renderItem: function (ul, item) {
            var div = $('<div class="pl-3">')
                .append($("<b>").text(item.label))
                .append(": ")
                .append(item.description);
            if (item.extras) {
                div.append(" (").append($("<i>").text(item.extras)).append(")");
            }
            return $("<li>")
                .append(div)
                .appendTo(ul);
        },
        /**
         * @param ul
         * @param {SearchResult[]} items
         */
        _renderMenu: function (ul, items) {
            var that = this;
            if (items.length === 1 && items[0].noresults) {
                ul.append($("<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold alert-danger'>")
                    .text(items[0].label));
                return;
            }
            var currentCategory = null;
            items.forEach(function (item) {
                var label = item.matchType + " : " + item.label;
                if (item.matchType !== currentCategory) {
                    ul.append($("<li class='ui-autocomplete-category my-1 p-2 font-weight-bold alert-info'>")
                        .attr("aria-label", label)
                        .text(item.matchType));
                    currentCategory = item.matchType;
                }
                var li = that._renderItemData(ul, item);
                if (item.matchType) {
                    li.attr("aria-label", label);
                }
            });
        }
    });

})();
