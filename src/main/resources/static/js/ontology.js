/* globals ontologyId */
/**
 * Ontology-related scripts.
 * @author poirigui
 */
(function () {
    'use strict';

    $('.import-ontology-btn').click(function () {
        $(this).find('.spinner').toggleClass('d-none', false);
    });

    $('.update-ontology-btn').click(function () {
        $(this).find('.spinner').toggleClass('d-none', false);
    });

    $('.delete-ontology-btn').click(function () {
        $(this).find('.spinner').toggleClass('d-none', false);
    });

    $('.ontology-term-autocomplete').autocomplete({
        minLength: 3,
        delay: 200,
        source: function (request, response) {
            var term = request.term;
            $.getJSON(contextPath + '/admin/ontologies/' + ontologyId + '/autocomplete-terms', {
                query: term
            }).done(function (data) {
                if (!data.length) {
                    return response([{
                        noresults: true,
                        label: 'No matches found for "' + term + '".',
                        value: term
                    }
                    ]);
                } else {
                    response(data);
                }
            }).fail(function () {
                response([{noresults: true, label: 'Error querying search endpoint.', value: term}]);
            });
        }
    });


    var renameOntologyTermInputName = function (attr, newIndex) {
        var parts = attr.split(/[\[\]]/, 3);
        return parts[0] + '[' + newIndex + ']' + parts[2];
    };

    /* dragging for reordering terms */
    var simpleOntologyForm = $('#simple-ontology-form');
    var ontologyTermRow;
    simpleOntologyForm.on('dragstart', '.ontology-term-row', function (event) {
        ontologyTermRow = event.target;
    });
    simpleOntologyForm.on('dragenter', '.ontology-term-row', function (event) {
        event.preventDefault();
        // only allowed relative to other rows
        var relativeTo = $(event.target).closest('.ontology-term-row');
        if (relativeTo) {
            var rows = simpleOntologyForm.find('.ontology-term-row');
            if (relativeTo.is(rows.first())) {
                relativeTo.before(ontologyTermRow);
            } else {
                relativeTo.after(ontologyTermRow);
            }
        }
    });
    simpleOntologyForm.on('dragover', '.ontology-term-row', function (event) {
        event.preventDefault();
    });
    simpleOntologyForm.on('dragend', '.ontology-term-row', function (event) {
        event.preventDefault();
        // TODO: restore element if drag is cancelled
        // TODO: fix input names to match the new order
        simpleOntologyForm.find('.ontology-term-row').each(function (rowIndex) {
            $(this).find('input')
                .attr('name', function (i, attr) {
                    return renameOntologyTermInputName(attr, rowIndex);
                });
        });
    });

    var idCounter = 1;
    $('.add-ontology-term-row-btn').click(function (event) {
        event.preventDefault();
        var rows = simpleOntologyForm.find('.ontology-term-row');
        // create a row based on the model of the first one
        var newRow = rows.first().clone();
        newRow.find('input')
            .attr('id', function (i, attr) {
                return attr + '_' + idCounter;
            })
            .attr('name', function (i, attr) {
                return renameOntologyTermInputName(attr, rows.length);
            })
            .val(function (i, val) {
                var inputName = $(this).attr('name');
                var inputType = $(this).attr('type');
                // FIXME: this is not the best way of detecting companion hidden input to checkboxes
                if (inputType === 'checkbox' || (inputType === 'hidden' && inputName.startsWith('_'))) {
                    return val;
                } else {
                    return '';
                }
            })
            .prop('checked', false);
        newRow.find('label').attr('for', function (i, attr) {
            return attr + '_' + idCounter;
        });
        // reset the term ID because it will be generated
        newRow.find('.ontology-term-id').html($('<em>').text('Term ID will be generated'));
        newRow.insertAfter(rows.last());
        idCounter += 1;
    });

    $(document).on('click', '.ontology-term-row .remove-ontology-term-row', function (event) {
        event.preventDefault();
        $(this).closest('.form-row').remove();
    });

    function ProgressPayload(processedElements, totalElements, elapsedTime) {
        this.processedElements = processedElements;
        this.totalElements = totalElements;
        this.elapsedTime = elapsedTime;
    }

    $('.update-reactome-pathway-summations-btn').click(function (event) {
        event.preventDefault();
        var reactomeOntologyId = $(this).data('reactome-ontology-id');
        var formGroup = $(this).closest('.form-group');
        var progressFeedback = formGroup.find('.progress-feedback');
        var evtSource = new window.EventSource('/admin/ontologies/' + reactomeOntologyId + '/update-reactome-pathway-summations');
        $(progressFeedback).toggleClass('text-success', false);
        evtSource.onmessage = function (e) {
            /**
             * @type {ProgressPayload}
             */
            var progressPayload = window.JSON.parse(e.data);
            if (progressPayload.processedElements === progressPayload.totalElements) {
                $(progressFeedback)
                    .toggleClass('text-success', true)
                    .text('Completed in ' + progressPayload.elapsedTime + ' seconds!');
            } else {
                $(progressFeedback).text('Processed ' + progressPayload.processedElements + ' out of ' + progressPayload.totalElements + ' pathways.');
            }
        };
    });
})();