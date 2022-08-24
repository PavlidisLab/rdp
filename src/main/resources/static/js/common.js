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

require('../css/common.scss');

var ResizeObserver = window.ResizeObserver;
var formUtil = require('./util/form');

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

    $(document).on("click", 'table.dataTable .data-table-delete-row', function (event) {
        event.preventDefault();
        var table = $(this).closest('table').DataTable();
        table.row($(this).closest('tr')).remove().draw();
    });

    /* we use a hide behaviour on alert instead of bootstrap defaults to remove the element from DOM */
    $('.alert > .close').on('click', function (event) {
        event.preventDefault();
        $(this).parent().hide();
    });

    $(document).on('click', '.badge > .close', function (event) {
        event.preventDefault();
        $(this).parent().remove();
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

    /**
     * @typedef {{id: Number, label: String, description: String, extras: String, match: Object, matchType: String}} SearchResult
     */

    /* autocomplete look & feel based on SearchResult */
    // noinspection JSUnusedGlobalSymbols
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
                .append($("<b>").text(item.label));
            if (item.description) {
                div.append(": ").append(item.description);
            }
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
                ul.append($("<li aria-label='noresults' class='ui-autocomplete-category p-2 font-weight-bold alert-danger'>")
                    .text(items[0].label));
                return;
            }
            var currentCategory = null;
            items.forEach(function (item) {
                var label = item.matchType + " : " + item.label;
                if (item.matchType && item.matchType !== currentCategory) {
                    ul.append($("<li class='ui-autocomplete-category p-2 font-weight-bold alert-info'>")
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

    /* adjust all sticky elements to appear after the staging banner */
    // unfortunately this cannot be done via CSS because the staging banner size can change
    $(document).ready(function () {
        var stagingBanner = document.getElementById('staging-banner');
        if (stagingBanner !== null) {
            new ResizeObserver(function () {
                document.querySelectorAll('.sticky-top').forEach(function (element) {
                    if (element !== stagingBanner) {
                        // there's an extra pixel line that shows up above the sticky
                        element.style.top = (stagingBanner.offsetHeight - 1) + 'px';
                    }
                });
            }).observe(stagingBanner);
        }
    });

    /* if the page has a main save button, intercept ctrl-s to trigger its action */
    $(document).ready(function () {
        var saveButton = document.getElementById('main-save-button');
        if (saveButton !== null) {
            document.addEventListener('keydown', function (event) {
                if (event.ctrlKey && event.key === 's') {
                    event.preventDefault();
                    saveButton.click();
                }
            });
        }
    });

    /* check if any important form changed in the UI */
    $(document).ready(function () {
        var importantForms = document.querySelectorAll('form.important');
        var initialData = {};
        importantForms.forEach(function (form) {
            initialData[form] = formUtil.serialize(form);
        });
        window.addEventListener('beforeunload', function (event) {
            var unsavedChanges = Array.from(importantForms).some(function (form) {
                return formUtil.serialize(form) !== initialData[form];
            });
            if (unsavedChanges) {
                event.preventDefault();
                event.returnValue = "There are unsaved changes in this page. Are you sure you want to leave?";
            }
        });
    });
})();
