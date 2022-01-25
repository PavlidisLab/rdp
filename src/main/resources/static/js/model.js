/* globals currentTaxonId, customizableGeneLevel, enabledGenePrivacyLevels, privacyLevels, userPrivacyLevel */
(function () {
    "use strict";

    var geneTable = $('#gene-table');
    var termTable = $('#term-table');

    function collectModel() {
        var model = {};

        // Research Information
        model.description = $.trim($('.research-info').find('.data-edit')[0].value);

        // Gene Information
        var geneTierMap = {};
        var genePrivacyLevelMap = {};

        geneTable.DataTable().rows().every(function () {
            var data = this.data();
            var geneId = parseInt(data[1], 10);
            if (!isNaN(geneId)) {
                geneTierMap[geneId] = $(this.node()).find('input[type=checkbox]').prop('checked') ? "TIER1" : "TIER2";
            }
            if (customizableGeneLevel) {
                var selectedPrivacyLevelOption = $(this.node()).find('select option:selected')[0];
                genePrivacyLevelMap[geneId] = selectedPrivacyLevelOption.value ? parseInt(selectedPrivacyLevelOption.value) : null;
            }
        });
        model.geneTierMap = geneTierMap;
        model.genePrivacyLevelMap = genePrivacyLevelMap;

        // Term Information
        var goIds = [];
        termTable.DataTable().rows().every(function () {
            var data = this.data();
            goIds.push($.trim($(data[0]).text()));
        });
        goIds.sort();
        model.goIds = goIds;

        return model;
    }

    function addGeneRow(data) {
        var table = geneTable.DataTable();
        var rows = [];
        for (var symbol in data) {
            var gene = data[symbol];
            var row = [];
            if (gene === null) {
                window.console.log("Issue obtaining metadata for: " + symbol);
                row.push('<span class="align-middle text-danger"><i class="delete-row align-middle"></i>' + symbol + '</span>');
                row.push('');
                row.push('<span class="align-middle text-danger">Could not find gene for provided gene identifier.</span>');
                row.push('');
                if (customizableGeneLevel) {
                    row.push('');
                }
            } else {
                /* gene is already in the table */
                if (table.column(1).data().toArray().includes(gene.geneId + '')) {
                    continue;
                }
                row.push('<span class="align-middle"><i class="delete-row align-middle"></i><a href="https://www.ncbi.nlm.nih.gov/gene/' + gene.geneId + '" target="_blank" class="align-middle" rel="noopener">' + gene.symbol + '</a></span>');
                row.push(gene.geneId + '');
                row.push('<span class="align-middle">' + gene.name + '</span>');
                row.push('<input name="primary" class="align-middle" type="checkbox"/>');
                if (customizableGeneLevel) {
                    var privacyOptions = enabledGenePrivacyLevels.map(function (k) {
                        var privacyLevel = privacyLevels[k];
                        if (privacyLevel.ordinal <= userPrivacyLevel.ordinal) {
                            return '<option value="' + privacyLevel.ordinal + '"' +
                                (privacyLevel.ordinal === userPrivacyLevel.ordinal ? ' selected' : '') + '>' +
                                privacyLevel.label + '</option>';
                        } else {
                            return '';
                        }
                    }).join('');
                    row.push('<select class="form-control">' + privacyOptions + '</select>');
                }
            }
            rows.push(row);
        }

        table.rows.add(rows).nodes()
            .to$()
            .addClass('new-row');

        table.columns.adjust().draw();

        return rows.length;
    }

    function addTermRow(data) {
        var table = termTable.DataTable();
        var rows = [];
        for (var goId in data) {
            var term = data[goId];
            var row = [];
            if (term === null) {
                window.console.log("Issue obtaining metadata for: " + goId);
                row.push('<span class="align-middle text-danger"><i class="delete-row align-middle"></i>' + goId + '</span>');
                row.push('<span class="align-middle text-danger">Could not find term for provided GO identifier.</span>');
                row.push('');
                row.push('');
                row.push('');
            } else {
                if (table.column(0).data().toArray().includes(term.goId)) {
                    continue;
                }
                row.push('<span class="align-middle">' +
                    '<i class="delete-row"></i>' +
                    '<a href="http://www.ebi.ac.uk/QuickGO/GTerm?id=' + encodeURIComponent(term.goId) + '" ' +
                    'target="_blank" data-toggle="tooltip" class="align-middle" title="' + term.definition + '">' + term.goId + '</a></span>');
                row.push('<span class="align-middle">' + term.name + '</span>');
                row.push('<span class="align-middle">' + term.aspect + '</span>');
                row.push('<a href="#" class="align-middle overlap-show-modal" data-toggle="modal" data-target="#overlapModal" data-go-id="' + term.goId + '">' + term.frequency + '</a>');
                row.push('<span class="align-middle">' + term.size + '</span>');
            }
            rows.push(row);
        }

        table.rows.add(rows).nodes()
            .to$()
            .addClass('new-row');
        // .find('[data-toggle="tooltip"]').tooltip();

        table.columns.adjust().draw();

        return rows.length;
    }

    $.fn.dataTable.ext.order['dom-checkbox'] = function (settings, col) {
        return this.api().column(col, {order: 'index'}).nodes().map(function (td) {
            return $('input', td).prop('checked') ? '1' : '0';
        });
    };

    var geneTableColumnDefs = [
        {"name": "Symbol", "targets": 0},
        {"name": "Id", "targets": 1, "visible": false},
        {"name": "Name", "targets": 2},
        {"name": "Primary", "targets": 3, "className": "text-center", "orderDataType": "dom-checkbox"}];

    if (customizableGeneLevel) {
        geneTableColumnDefs.push({
            "name": "PrivacyLevel",
            "targets": 4,
            "className": "text-center",
            "orderDataType": "dom-select"
        });
    }

    geneTable.DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "desc"]],
        "columnDefs": geneTableColumnDefs
    });

    termTable.DataTable({
        "scrollY": "200px",
        "scrollCollapse": true,
        "paging": false,
        "searching": false,
        "info": false,
        "order": [[0, "desc"]]
    });

    var initialModel = collectModel();

    // Enable navigation prompt
    window.onbeforeunload = function () {
        return JSON.stringify(initialModel) !== JSON.stringify(collectModel()) ? true : undefined;
    };

    $(document).on("click", ".save-model", function () {
        var model = collectModel();

        var spinner = $(this).find('.spinner');
        spinner.removeClass("d-none");

        $.ajax({
            type: "POST",
            url: window.location.href,
            data: JSON.stringify(model),
            contentType: "application/json",
            success: function () {
                $('.success-row').show();
                $('.error-row').hide();
                spinner.toggleClass("d-none", true);
                initialModel = collectModel();
                $('.new-row').removeClass("new-row");
            },
            error: function () {
                $('.success-row').hide();
                $('.error-row').show();
                spinner.toggleClass("d-none", false);
            }
        });
    });

    $(document).on("keypress", ".autocomplete", function (e) {
        if (e.which === 13) {
            $(this).closest('.input-group').find('button.add-row').click();
        }
    });

    // Genes

    $(function () {
        var cache = {};
        var autocomplete = $("#genes").find(".autocomplete");
        autocomplete.autocomplete({
            minLength: 2,
            delay: 500,
            source: function (request, response) {
                var term = request.term;
                if (term in cache) {
                    response(cache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }

                $.getJSON("/taxon/" + encodeURIComponent(currentTaxonId) + "/gene/search", {query: term, max: 10})
                    .done(function (data) {
                        if (!data.length) {
                            data = [
                                {
                                    noresults: true,
                                    label: 'No matches found',
                                    value: term
                                }
                            ];
                        }
                        cache[term] = data;
                        response(data);
                    })
                    .fail(function () {
                        response([{noresults: true, label: 'Error querying search endpoint.', value: term}]);
                    });
            },
            select: function (event, ui) {
                autocomplete.val(ui.item.match.symbol);
                return false;
            }
        });
        autocomplete.autocomplete("instance")._renderItem = function (ul, item) {
            return $("<li>")
                .append("<div class='pl-3'><b>" + item.match.symbol + "</b>: " + item.match.name + " (<i>" + item.match.aliases + "</i>)</div>")
                .appendTo(ul);
        };
        autocomplete.autocomplete("instance")._create = function () {
            this._super();
            this.widget().menu("option", "items", "> :not(.ui-autocomplete-category)");
        };
        autocomplete.autocomplete("instance")._renderMenu = function (ul, items) {
            var that = this,
                currentCategory = "";

            if (items.length === 1 && items[0].noresults) {
                ul.append("<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #fddce5; font-size: 1rem;'>No Results</li>");
                return;
            }

            $.each(items, function (index, item) {
                var li;
                var label = item.matchType + " : " + item.match.symbol;
                if (item.matchType !== currentCategory) {
                    ul.append("<li aria-label='" + label + "' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #e3f2fd; font-size: 1rem;'>" + item.matchType + "</li>");
                    currentCategory = item.matchType;
                }
                li = that._renderItemData(ul, item);
                if (item.matchType) {
                    li.attr("aria-label", label);
                }
            });
        };

    });

    $("#genes").on("click", ".add-row", function () {

        var line = $.trim($(this).closest('.input-group').find('input').val());

        var symbols = line.split(",").map(function (item) {
            return item.trim();
        }).filter(function (item) {
            return item;
        });

        if (symbols.length === 0) {
            return;
        }

        var self = $(this);
        var spinner = self.find('.spinner');
        spinner.removeClass("d-none");

        var encodedSymbols = symbols.reduce(function (encoded, symbol) {
            return encoded + (encoded === '' ? '' : '&') + 'symbols' + '=' + encodeURIComponent(symbol);
        }, '');

        $.ajax({
            type: 'GET',
            url: '/taxon/' + encodeURIComponent(currentTaxonId) + '/gene/search',
            data: encodedSymbols
        })
            .done(addGeneRow)
            .always(function () {
                spinner.toggleClass('d-none', true);
            });
        $(this).closest('.input-group').find('input').val("");
    });

    // Terms

    $(function () {
        var cache = {};
        var autocomplete = $("#terms").find(".autocomplete");
        autocomplete.autocomplete({
            minLength: 2,
            delay: 500,
            source: function (request, response) {
                var term = request.term;
                if (term in cache) {
                    response(cache[term]);
                    return;
                }

                if (term.includes(",")) {
                    return;
                }
                $.getJSON("/taxon/" + encodeURIComponent(currentTaxonId) + "/term/search", {query: term, max: 10})
                    .done(function (data) {
                        if (!data.length) {
                            data = [
                                {
                                    noresults: true,
                                    label: 'No matches found',
                                    value: term
                                }
                            ];
                        }
                        cache[term] = data;
                        response(data);
                    })
                    .fail(function () {
                        response([{noresults: true, label: 'Error querying search endpoint.', value: term}]);
                    });
            },
            select: function (event, ui) {
                autocomplete.val(ui.item.match.goId);
                return false;
            }
        });
        autocomplete.autocomplete("instance")._renderItem = function (ul, item) {
            return $("<li>")
                .append("<div class='pl-3'><b>" + item.match.goId + "</b>: " + item.match.name + " (Size: " + item.match.size + ")</div>")
                .appendTo(ul);
        };
        autocomplete.autocomplete("instance")._create = function () {
            this._super();
            this.widget().menu("option", "items", "> :not(.ui-autocomplete-category)");
        };
        autocomplete.autocomplete("instance")._renderMenu = function (ul, items) {
            var that = this,
                currentCategory = "";

            if (items.length === 1 && items[0].noresults) {
                ul.append("<li aria-label='noresults' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #fddce5; font-size: 1rem;'>No Results</li>");
                return;
            }

            $.each(items, function (index, item) {
                var li;
                var label = item.matchType + " : " + item.match.goId;
                if (item.matchType !== currentCategory) {
                    ul.append("<li aria-label='" + label + "' class='ui-autocomplete-category my-1 p-2 font-weight-bold' style='background-color: #e3f2fd; font-size: 1rem;'>" + item.matchType + "</li>");
                    currentCategory = item.matchType;
                }
                li = that._renderItemData(ul, item);
                if (item.matchType) {
                    li.attr("aria-label", label);
                }
            });
        };

    });

    $("#terms").on("click", ".add-row", function () {

        var line = $.trim($(this).closest('.input-group').find('input').val());

        var ids = line.split(",").map(function (item) {
            return item.trim();
        }).filter(function (item) {
            return item;
        });

        if (ids.length === 0) {
            return;
        }

        var self = $(this);

        var spinner = self.find('.spinner');
        spinner.removeClass("d-none");

        var encodedGoIds = ids.reduce(function (encoded, id) {
            return encoded + (encoded === '' ? '' : '&') + 'goIds' + '=' + encodeURIComponent(id);
        }, '');


        $.ajax({
            type: 'GET',
            url: '/user/taxon/' + encodeURIComponent(currentTaxonId) + '/term/search',
            data: encodedGoIds
        })
            .done(addTermRow)
            .always(function () {
                spinner.toggleClass("d-none", true);
            });
        $(this).closest('.input-group').find('input').val("");
    });

    $('.recommend-terms').click(function () {
        var spinner = $(this).find('.spinner');
        var recommendMessage = $('#terms').find('.recommend-message');
        spinner.toggleClass("d-none", false);
        recommendMessage.toggleClass('d-none', true);
        $.getJSON("/user/taxon/" + encodeURIComponent(currentTaxonId) + "/term/recommend")
            .done(function (data) {
                var addedTerms = addTermRow(data);
                if (addedTerms > 0) {
                    recommendMessage
                        .text('Recommended ' + addedTerms + ' terms.')
                        .toggleClass('alert-success', true)
                        .toggleClass('alert-danger', false);
                } else {
                    recommendMessage
                        .text('Could not recommend new terms. Try adding more genes and make sure you click the save button.')
                        .toggleClass('alert-success', false)
                        .toggleClass('alert-danger', true);
                }
            })
            .fail(function () {
                recommendMessage
                    .text('There was an error in attempting to retrieve recommended terms.')
                    .toggleClass('alert-success', false)
                    .toggleClass('alert-danger', true);
            })
            .always(function () {
                spinner.toggleClass("d-none", true);
                recommendMessage.toggleClass('d-none', false);
                $('#terms-tab').tab('show');
            });
    });

    $('#overlapModal').on('show.bs.modal', function (e) {
        var goId = $(e.relatedTarget).data('go-id');
        $("#overlapModal").find(".modal-body").load("/user/taxon/" + encodeURIComponent(currentTaxonId) + "/term/" + encodeURIComponent(goId) + "/gene/view");
    });

    $('#terms-tab').on('shown.bs.tab', function () {
        termTable.DataTable().draw();
    });
})();